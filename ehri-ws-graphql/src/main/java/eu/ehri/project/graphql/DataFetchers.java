/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.graphql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.LanguageHelpers;
import graphql.schema.DataFetcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static eu.ehri.project.graphql.Cursors.*;
import static eu.ehri.project.graphql.GraphQLConstants.*;

/**
 * Factories for the {@link DataFetcher}s that resolve schema fields against
 * the underlying {@link Api}. Fetchers that don't need per-request state
 * (the current API/accessor or whether this is a streaming query) are plain
 * static fields/methods; the rest are instance methods bound to the request's
 * {@code Api} and streaming flag.
 */
final class DataFetchers {

    private static final Config config = ConfigFactory.load();

    private static final List<EntityClass> supportedTypes = ImmutableList.of(
            EntityClass.DOCUMENTARY_UNIT,
            EntityClass.REPOSITORY,
            EntityClass.COUNTRY,
            EntityClass.HISTORICAL_AGENT,
            EntityClass.CVOC_CONCEPT,
            EntityClass.CVOC_VOCABULARY,
            EntityClass.AUTHORITATIVE_SET,
            EntityClass.ANNOTATION,
            EntityClass.LINK
    );

    private static final List<EventTypes> supportedEvents = ImmutableList.of(
            EventTypes.creation,
            EventTypes.modification,
            EventTypes.deletion,
            EventTypes.ingest,
            EventTypes.annotation
    );

    private final Api api;
    private final boolean stream;

    DataFetchers(Api api, boolean stream) {
        this.api = api;
        this.stream = stream;
    }

    private Api api() {
        return api;
    }

    private EventsApi events() {
        return api().events()
                .withEntityClasses(supportedTypes.toArray(new EntityClass[0]))
                .withEventTypes(supportedEvents.toArray(new EventTypes[0]));
    }

    DataFetcher<Iterable<SystemEvent>> itemEventsDataFetcher() {
        return env -> events().listForItem(env.<Entity>getSource().as(SystemEvent.class));
    }

    DataFetcher<Map<String, Object>> docDataFetcher() {
        return env -> env.getArgument(TOP_LEVEL_PARAM)
                ? topLevelDocDataFetcher().get(env)
                : entityTypeConnectionDataFetcher(EntityClass.DOCUMENTARY_UNIT).get(env);
    }

    DataFetcher<Map<String, Object>> topLevelDocDataFetcher() {
        return connectionDataFetcher(() -> {
            Iterable<Country> countries = api().query()
                    .withStreaming(true).withLimit(-1).page(EntityClass.COUNTRY, Country.class);
            Iterable<Iterable<Entity>> docs = Iterables.transform(countries, c ->
                    Iterables.transform(c.getTopLevelDocumentaryUnits(), d -> d.as(Entity.class)));
            return Iterables.concat(docs);
        });
    }

    DataFetcher<Map<String, Object>> entityTypeConnectionDataFetcher(EntityClass type) {
        // FIXME: The only way to get a list of all items of a given
        // type via the API alone is to run a query as a stream w/ no limit.
        // However, this means that ACL filtering will be applied twice,
        // once here, and once by the connection data fetcher, which also
        // applies pagination. This is a bit gross but the speed difference
        // appears to be negligible.
        return connectionDataFetcher(() -> api().query()
                .withStreaming(true).withLimit(-1).page(type, Entity.class));
    }

    DataFetcher<Map<String, Object>> hierarchicalOneToManyRelationshipConnectionFetcher(
            Function<Entity, Iterable<? extends Entity>> top, Function<Entity, Iterable<? extends Entity>> all) {
        // Depending on the value of the "all" argument, return either just
        // the top level items or everything in the tree.
        return env -> {
            boolean allOrTop = (Boolean) Optional.ofNullable(env.getArgument(ALL_PARAM)).orElse(false);
            Function<Entity, Iterable<? extends Entity>> func = allOrTop ? all : top;
            return connectionDataFetcher(() -> func.apply((env.getSource()))).get(env);
        };
    }

    DataFetcher<Map<String, Object>> oneToManyRelationshipConnectionFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return env -> connectionDataFetcher(() -> f.apply((env.getSource()))).get(env);
    }

    DataFetcher<Map<String, Object>> connectionDataFetcher(Supplier<Iterable<? extends Entity>> iter) {
        // NB: The data fetcher takes a supplier here so lazily generated
        // streams can be invoked more than one (if, e.g. both the items array
        // and the edges array is needed.) Otherwise we would have to somehow
        // reset the Iterable.
        return env -> {
            int limit = getLimit(env.getArgument(FIRST_PARAM), stream);
            int offset = getOffset(env.getArgument(AFTER_PARAM), env.getArgument(FROM_PARAM));
            return stream && limit < 0
                    ? lazyConnectionData(iter, limit, offset)
                    : strictConnectionData(iter, limit, offset);
        };
    }

    private Map<String, Object> connectionData(Iterable<?> items,
                                               Iterable<Map<String, Object>> edges, String nextCursor, String prevCursor) {
        return mapOf(
                ITEMS, items,
                EDGES, edges,
                PAGE_INFO, mapOf(
                        HAS_NEXT_PAGE, nextCursor != null,
                        NEXT_PAGE, nextCursor,
                        HAS_PREVIOUS_PAGE, prevCursor != null,
                        PREVIOUS_PAGE, prevCursor
                )
        );
    }

    private Map<String, Object> strictConnectionData(Supplier<Iterable<? extends Entity>> iter, int limit, int offset) {
        // Note: strict connections are considerably slower than lazy ones
        // since to assemble the PageInfo we need to count the total number
        // of items, which involves fetching the iterator twice.
        long total = Iterables.size(iter.get());
        QueryApi query = api().query().withStreaming(true).withLimit(limit).withOffset(offset);
        QueryApi.Page<Entity> page = query.page(iter.get(), Entity.class);
        List<Entity> items = Lists.newArrayList(page);

        // Create a list of edges, with the cursor taking into
        // account each item's offset
        List<Map<String, Object>> edges = Lists.newArrayListWithExpectedSize(items.size());
        for (int i = 0; i < items.size(); i++) {
            edges.add(mapOf(
                    CURSOR, toBase64(String.valueOf(offset + i)),
                    NODE, items.get(i)
            ));
        }

        boolean hasNext = page.getOffset() + items.size() < total;
        boolean hasPrev = page.getOffset() > 0;
        String nextCursor = toBase64(String.valueOf(offset + limit));
        String prevCursor = toBase64(String.valueOf(offset - limit));
        return connectionData(items, edges, hasNext ? nextCursor : null, hasPrev ? prevCursor : null);
    }

    private Map<String, Object> lazyConnectionData(Supplier<Iterable<? extends Entity>> iter, int limit, int offset) {
        QueryApi query = api().query().withLimit(limit).withOffset(offset);
        QueryApi.Page<Entity> items = query.page(iter.get(), Entity.class);
        boolean hasPrev = items.getOffset() > 0;

        // Create a list of edges, with the cursor taking into
        // account each item's offset
        final AtomicInteger index = new AtomicInteger();
        Iterable<Map<String, Object>> edges = Iterables.transform(
                query.page(iter.get(), Entity.class), item -> mapOf(
                        CURSOR, toBase64(String.valueOf(offset + index.getAndIncrement())),
                        NODE, item
                ));

        String prevCursor = toBase64(String.valueOf(offset - limit));
        return connectionData(items, edges, null, hasPrev ? prevCursor : null);
    }

    DataFetcher<Entity> entityIdDataFetcher(String type) {
        return env -> {
            try {
                Accessible detail = api().get(env.getArgument(Bundle.ID_KEY), Accessible.class);
                return Objects.equals(detail.getType(), type) ? detail : null;
            } catch (ItemNotFound e) {
                return null;
            }
        };
    }

    DataFetcher<Entity> entityPidDataFetcher() {
        return env -> {
            try {
                return api().getByPid(env.getArgument("pid"), PersistentIdentifiable.class);
            } catch (ItemNotFound e) {
                return null;
            }
        };
    }

    static final DataFetcher<String> idDataFetcher =
            env -> (env.<Entity>getSource()).getProperty(EntityType.ID_KEY);

    static final DataFetcher<String> typeDataFetcher =
            env -> (env.<Entity>getSource()).getProperty(EntityType.TYPE_KEY);

    static final DataFetcher<String> pidDataFetcher =
            env -> (env.<Entity>getSource()).getProperty(Ontology.PID_KEY);

    static final DataFetcher<String> arkDataFetcher = env -> {
        final String pid = pidDataFetcher.get(env);
        return pid == null ? null : config.getString("io.pids.prefix") + pid;
    };

    static final DataFetcher<Object> attributeDataFetcher = env -> {
        Entity source = env.getSource();
        String name = env.getMergedField().getName();
        return source.getProperty(name);
    };

    static DataFetcher<Object> keyDataFetcher(String key) {
        return env -> {
            Entity source = env.getSource();
            return source.getProperty(key);
        };
    }

    static DataFetcher<List<?>> listDataFetcher(DataFetcher<?> fetcher) {
        return env -> {
            Object obj = fetcher.get(env);
            if (obj == null) {
                return Collections.emptyList();
            } else if (obj instanceof List) {
                return (List<?>) obj;
            } else {
                return Lists.newArrayList(obj);
            }
        };
    }

    static final DataFetcher<Description> descriptionDataFetcher = env -> {
        String lang = env.getArgument(Ontology.LANGUAGE_OF_DESCRIPTION);
        String lang2 = env.getArgument(LANG_PARAM);
        String code = env.getArgument(Ontology.IDENTIFIER_KEY);

        Entity source = env.getSource();
        Iterable<Description> descriptions = source.as(Described.class).getDescriptions();

        if (lang == null && lang2 == null && code == null) {
            int at = env.getArgument(SLICE_PARAM);
            List<Description> descList = Lists.newArrayList(descriptions);
            return at >= 1 && descList.size() >= at ? descList.get(at - 1) : null;
        } else {
            String checkedCode = LanguageHelpers.convertCode(lang2).orElse(lang);
            for (Description next : descriptions) {
                String langCode = next.getLanguageOfDescription();
                if (langCode != null && langCode.equalsIgnoreCase(checkedCode)) {
                    if (code != null && !code.isEmpty()) {
                        String ident = next.getDescriptionCode();
                        if (code.equals(ident)) {
                            return next;
                        }
                    } else {
                        return next;
                    }
                }
            }
            return null;
        }
    };

    static final DataFetcher<String> annotationNameDataFetcher =
            env -> Optional.ofNullable(env.<Entity>getSource().as(Annotation.class)
                    .getAnnotator())
                    .map(Named::getName).orElse(null);

    static final DataFetcher<List<Map<String, Object>>> connectedItemsDataFetcher = env -> {
        String linkType = env.getArgument("linkType");
        Entity source = env.getSource();
        Iterable<Link> links = source.as(Linkable.class).getLinks();
        return StreamSupport.stream(links.spliterator(), false)
                .filter(link -> linkType == null || linkType.equals(link.getLinkType().toString()))
                .map(link -> {
            Linkable target = Iterables.tryFind(link.getLinkTargets(),
                    t -> t != null && !t.equals(source)).orNull();
            return target == null ? null : mapOf("context", link, "item", target);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    };

    static <S, T> DataFetcher<T> transformingDataFetcher(DataFetcher<S> fetcher, Function<S, T> transformer) {
        return env -> transformer.apply(fetcher.get(env));
    }

    DataFetcher<Iterable<Entity>> oneToManyRelationshipFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return env -> {
            Iterable<? extends Entity> elements = f.apply((env.getSource()));
            return api().query().withStreaming(true).withLimit(-1).page(elements, Entity.class);
        };
    }

    DataFetcher<Entity> manyToOneRelationshipFetcher(Function<Entity, Entity> f) {
        return env -> {
            Entity elem = f.apply(env.getSource());
            if (elem != null &&
                    AclManager.getAclFilterFunction(api().accessor())
                            .compute(elem.asVertex())) {
                return elem;
            }

            return null;
        };
    }

    DataFetcher<Integer> itemCountDataFetcher(Function<Entity, Integer> f) {
        return env -> Math.toIntExact(f.apply(env.getSource()));
    }
}
