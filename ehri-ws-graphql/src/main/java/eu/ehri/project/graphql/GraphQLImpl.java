/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.graphql;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.ContactInfo;
import eu.ehri.project.definitions.CountryInfo;
import eu.ehri.project.definitions.DefinitionList;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Geo;
import eu.ehri.project.definitions.Isaar;
import eu.ehri.project.definitions.IsadG;
import eu.ehri.project.definitions.Isdiah;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.definitions.Skos;
import eu.ehri.project.definitions.SkosMultilingual;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.Named;
import eu.ehri.project.models.base.Temporal;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.LanguageHelpers;
import graphql.TypeResolutionEnvironment;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Implementation of a GraphQL schema over the API
 */
public class GraphQLImpl {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("eu.ehri.project.graphql.messages");
    private static final Logger logger = LoggerFactory.getLogger(GraphQLImpl.class);

    private static final String SLICE_PARAM = "at";
    private static final String FIRST_PARAM = "first";
    private static final String FROM_PARAM = "from";
    private static final String AFTER_PARAM = "after";
    private static final String ALL_PARAM = "all";
    private static final String TOP_LEVEL_PARAM = "topLevel";

    private static final String HAS_PREVIOUS_PAGE = "hasPreviousPage";
    private static final String HAS_NEXT_PAGE = "hasNextPage";
    private static final String PAGE_INFO = "pageInfo";
    private static final String ITEMS = "items";
    private static final String EDGES = "edges";
    private static final String NODE = "node";
    private static final String CURSOR = "cursor";
    private static final String NEXT_PAGE = "nextPage";
    private static final String PREVIOUS_PAGE = "previousPage";

    private static final int DEFAULT_LIST_LIMIT = 40;
    private static final int MAX_LIST_LIMIT = 100;

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

    private final Api _api;
    private final boolean stream;

    public GraphQLImpl(Api api, boolean stream) {
        this._api = api;
        this.stream = stream;
    }

    public GraphQLImpl(Api api) {
        this(api, false);
    }

    public GraphQLSchema getSchema() {
        return GraphQLSchema.newSchema()
                .query(queryType())
                // NB: this needed because the date type is only
                // references via a type reference to avoid forward-
                // declaration problems...
                .additionalTypes(Sets.newHashSet(datePeriodType, systemEventType))
                .build();
    }

    private Api api() {
        return _api;
    }

    private EventsApi events() {
        return api().events()
                .withEntityClasses(supportedTypes.toArray(new EntityClass[supportedTypes.size()]))
                .withEventTypes(supportedEvents.toArray(new EventTypes[supportedEvents.size()]));
    }

    private static String __(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.error("Missing resource for i18n key: {}", key);
            return '!' + key + '!';
        }

    }

    private static String toBase64(String s) {
        return Base64.encodeBase64String(s.getBytes());
    }

    private static String fromBase64(String s) {
        return new String(Base64.decodeBase64(s));
    }


    private static Map<String, Object> mapOf(Object... items) {
        Preconditions.checkArgument(items.length % 2 == 0, "Items must be pairs of key/value");
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < items.length; i += 2) {
            map.put(((String) items[i]), items[i + 1]);
        }
        return map;
    }

    private static int decodeCursor(String cursor, int defaultVal) {
        try {
            return cursor != null
                    ? Math.max(-1, Integer.parseInt(fromBase64(cursor)))
                    : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static int getLimit(Integer limitArg, boolean stream) {
        if (limitArg == null) {
            return stream ? -1 : DEFAULT_LIST_LIMIT;
        } else if (limitArg < 0) {
            return stream ? limitArg : MAX_LIST_LIMIT;
        } else {
            return stream ? limitArg : Math.min(MAX_LIST_LIMIT, limitArg);
        }
    }

    private static int getOffset(String afterCursor, String fromCursor) {
        if (afterCursor != null) {
            return decodeCursor(afterCursor, -1) + 1;
        } else if (fromCursor != null) {
            return decodeCursor(fromCursor, 0);
        } else {
            return 0;
        }
    }

    // Argument helpers...

    private static final GraphQLList GraphQLStringList = new GraphQLList(GraphQLString);
    private static final GraphQLNonNull GraphQLNonNullString = new GraphQLNonNull(GraphQLString);

    private static final GraphQLScalarType CursorType =
            new GraphQLScalarType("Cursor", __("cursor.description"), GraphQLString.getCoercing());

    private static final GraphQLArgument idArgument = newArgument()
            .name(Bundle.ID_KEY)
            .description(__("graphql.argument.id.description"))
            .type(new GraphQLNonNull(GraphQLID))
            .build();

    // Data fetchers...

    private DataFetcher<Iterable<SystemEvent>> itemEventsDataFetcher() {
        return env -> events().listForItem(env.<Entity>getSource().as(SystemEvent.class));
    }

    private DataFetcher<Map<String, Object>> docDataFetcher() {
        return env -> env.getArgument(TOP_LEVEL_PARAM)
                ? topLevelDocDataFetcher().get(env)
                : entityTypeConnectionDataFetcher(EntityClass.DOCUMENTARY_UNIT).get(env);
    }

    private DataFetcher<Map<String, Object>> topLevelDocDataFetcher() {
        return connectionDataFetcher(() -> {
            Iterable<Country> countries = api().query()
                    .withStreaming(true).withLimit(-1).page(EntityClass.COUNTRY, Country.class);
            Iterable<Iterable<Entity>> docs = Iterables.transform(countries, c ->
                    Iterables.transform(c.getTopLevelDocumentaryUnits(), d -> d.as(Entity.class)));
            return Iterables.concat(docs);
        });
    }

    private DataFetcher<Map<String, Object>> entityTypeConnectionDataFetcher(EntityClass type) {
        // FIXME: The only way to get a list of all items of a given
        // type via the API alone is to run a query as a stream w/ no limit.
        // However, this means that ACL filtering will be applied twice,
        // once here, and once by the connection data fetcher, which also
        // applies pagination. This is a bit gross but the speed difference
        // appears to be negligible.
        return connectionDataFetcher(() -> api().query()
                .withStreaming(true).withLimit(-1).page(type, Entity.class));
    }

    private DataFetcher<Map<String, Object>> hierarchicalOneToManyRelationshipConnectionFetcher(
            Function<Entity, Iterable<? extends Entity>> top, Function<Entity, Iterable<? extends Entity>> all) {
        // Depending on the value of the "all" argument, return either just
        // the top level items or everything in the tree.
        return env -> {
            boolean allOrTop = (Boolean) Optional.ofNullable(env.getArgument(ALL_PARAM)).orElse(false);
            Function<Entity, Iterable<? extends Entity>> func = allOrTop ? all : top;
            return connectionDataFetcher(() -> func.apply((env.getSource()))).get(env);
        };
    }

    private DataFetcher<Map<String, Object>> oneToManyRelationshipConnectionFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return env -> connectionDataFetcher(() -> f.apply((env.getSource()))).get(env);
    }

    private DataFetcher<Map<String, Object>> connectionDataFetcher(Supplier<Iterable<? extends Entity>> iter) {
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

    private DataFetcher<Entity> entityIdDataFetcher(String type) {
        return env -> {
            try {
                Accessible detail = api().get(env.getArgument(Bundle.ID_KEY), Accessible.class);
                return Objects.equals(detail.getType(), type) ? detail : null;
            } catch (ItemNotFound e) {
                return null;
            }
        };
    };

    private static final DataFetcher<String> idDataFetcher =
            env -> (env.<Entity>getSource()).getProperty(EntityType.ID_KEY);

    private static final DataFetcher<String> typeDataFetcher =
            env -> (env.<Entity>getSource()).getProperty(EntityType.TYPE_KEY);

    private static final DataFetcher<Object> attributeDataFetcher = env -> {
        Entity source = env.<Entity>getSource();
        String name = env.getFields().get(0).getName();
        return source.getProperty(name);
    };

    private static DataFetcher<List<?>> listDataFetcher(DataFetcher<?> fetcher) {
        return env -> {
            Object obj = fetcher.get(env);
            if (obj == null) {
                return Collections.emptyList();
            } else if (obj instanceof List) {
                return (List<?>)obj;
            } else {
                return Lists.newArrayList(obj);
            }
        };
    }

    private static final DataFetcher<Description> descriptionDataFetcher = env -> {
        String lang = env.getArgument(Ontology.LANGUAGE_OF_DESCRIPTION);
        String code = env.getArgument(Ontology.IDENTIFIER_KEY);

        Entity source = env.getSource();
        Iterable<Description> descriptions = source.as(Described.class).getDescriptions();

        if (lang == null && code == null) {
            int at = env.getArgument(SLICE_PARAM);
            List<Description> descList = Lists.newArrayList(descriptions);
            return at >= 1 && descList.size() >= at ? descList.get(at - 1) : null;
        } else {
            for (Description next : descriptions) {
                String langCode = next.getLanguageOfDescription();
                if (langCode.equalsIgnoreCase(lang)) {
                    if (code != null && !code.isEmpty()) {
                        String ident = next.getDescriptionCode();
                        if (ident.equals(code)) {
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

    private static final DataFetcher<String> annotationNameDataFetcher =
            env -> Optional.ofNullable(env.<Entity>getSource().as(Annotation.class)
                    .getAnnotator())
                    .map(Named::getName).orElse(null);

    private static final DataFetcher<List<Map<String, Object>>> relatedItemsDataFetcher = env -> {
        Entity source = env.getSource();
        Iterable<Link> links = source.as(Linkable.class).getLinks();
        return StreamSupport.stream(links.spliterator(), false).map(link -> {
            Linkable target = Iterables.tryFind(link.getLinkTargets(),
                    t -> t != null && !t.equals(source)).orNull();
            return target == null ? null : mapOf("context", link, "item", target);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    };

    private static <S, T> DataFetcher<T> transformingDataFetcher(DataFetcher<S> fetcher, Function<S, T> transformer) {
        return env -> transformer.apply(fetcher.get(env));
    }

    private DataFetcher<Iterable<Entity>> oneToManyRelationshipFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return env -> {
            Iterable<? extends Entity> elements = f.apply((env.getSource()));
            return api().query().withStreaming(true).withLimit(-1).page(elements, Entity.class);
        };
    }

    private DataFetcher<Entity> manyToOneRelationshipFetcher(Function<Entity, Entity> f) {
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

    // Field definition helpers...

    private static GraphQLFieldDefinition.Builder nullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description)
                .dataFetcher(attributeDataFetcher);
    }

    private static GraphQLFieldDefinition.Builder nullAttr(String name, String description) {
        return nullAttr(name, description, GraphQLString);
    }

    private static GraphQLFieldDefinition.Builder nonNullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description)
                .dataFetcher(attributeDataFetcher);
    }

    private static GraphQLFieldDefinition.Builder nonNullAttr(String name, String description) {
        return nonNullAttr(name, description, GraphQLString);
    }

    private static List<GraphQLFieldDefinition> nullStringAttrs(DefinitionList[] items) {
        return Lists.newArrayList(items)
                .stream().filter(i -> !i.isMultiValued())
                .map(f ->
                        newFieldDefinition()
                                .type(GraphQLString)
                                .name(f.name())
                                .description(f.getDescription())
                                .dataFetcher(attributeDataFetcher)
                                .build()
                ).collect(Collectors.toList());
    }

    private static List<GraphQLFieldDefinition> listStringAttrs(DefinitionList[] items) {
        return Lists.newArrayList(items)
                .stream().filter(DefinitionList::isMultiValued)
                .map(f ->
                        newFieldDefinition()
                                .type(GraphQLStringList)
                                .name(f.name())
                                .description(f.getDescription())
                                .dataFetcher(listDataFetcher(attributeDataFetcher))
                                .build()
                ).collect(Collectors.toList());
    }

    private static final GraphQLFieldDefinition.Builder idField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name(Bundle.ID_KEY)
            .description(__("graphql.field.id.description"))
            .dataFetcher(idDataFetcher);

    private static final GraphQLFieldDefinition.Builder typeField = newFieldDefinition()
            .type(new GraphQLNonNull(GraphQLString))
            .name(Bundle.TYPE_KEY)
            .description(__("graphql.field.type.description"))
            .dataFetcher(typeDataFetcher);

    private static GraphQLFieldDefinition.Builder singleDescriptionFieldDefinition(GraphQLOutputType descriptionType) {
        return newFieldDefinition()
                .type(descriptionType)
                .name("description")
                .argument(newArgument()
                        .name(Ontology.LANGUAGE_OF_DESCRIPTION)
                        .description(__("graphql.argument.languageCode.description"))
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name(Ontology.IDENTIFIER_KEY)
                        .description(__("graphql.argument.identifier.description"))
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name(SLICE_PARAM)
                        .description(__("graphql.argument.at.description"))
                        .type(GraphQLInt)
                        .defaultValue(1)
                        .build()
                )
                .description(__("graphl.field.description.description"))
                .dataFetcher(descriptionDataFetcher);
    }

    private static GraphQLFieldDefinition.Builder listFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher<Iterable<Entity>> dataFetcher) {
        return newFieldDefinition()
                .name(name)
                .type(new GraphQLList(type))
                .description(description)
                .dataFetcher(dataFetcher);
    }

    private GraphQLFieldDefinition.Builder itemEventsFieldDefinition() {
        return newFieldDefinition()
                .name("systemEvents")
                .description(__("graphql.field.systemEvents.description"))
                .type(new GraphQLList(new GraphQLTypeReference(Entities.SYSTEM_EVENT)))
                .dataFetcher(itemEventsDataFetcher());
    }

    private static GraphQLFieldDefinition.Builder connectionFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher<Map<String, Object>> dataFetcher, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .description(description)
                .type(type)
                .dataFetcher(dataFetcher)
                .argument(newArgument()
                        .name(FIRST_PARAM)
                        .type(GraphQLInt)
                        .description(__("graphql.argument.first.description"))
                        .build()
                )
                .argument(newArgument()
                        .name(AFTER_PARAM)
                        .description(__("graphql.argument.after.description"))
                        .type(CursorType)
                        .build()
                )
                .argument(newArgument()
                        .name(FROM_PARAM)
                        .description(__("graphql.argument.from.description"))
                        .type(CursorType)
                        .build()
                )
                .argument(Lists.newArrayList(arguments));
    }

    private static final List<GraphQLFieldDefinition> entityFields =
            ImmutableList.of(idField.build(), typeField.build());

    private static final List<GraphQLFieldDefinition> geoFields = ImmutableList.of(
            newFieldDefinition()
                    .name(Geo.latitude.name())
                    .description(Geo.latitude.getDescription())
                    .type(GraphQLBigDecimal)
                    .dataFetcher(attributeDataFetcher)
                    .build(),
            newFieldDefinition()
                    .name(Geo.longitude.name())
                    .description(Geo.longitude.getDescription())
                    .type(GraphQLBigDecimal)
                    .dataFetcher(attributeDataFetcher)
                    .build()
    );

    private static List<GraphQLFieldDefinition> descriptionFields() {
        return Lists.newArrayList(
                nonNullAttr(Ontology.LANGUAGE_OF_DESCRIPTION, __("graphql.field.languageCode.description")).build(),
                nonNullAttr(Ontology.NAME_KEY, __("graphql.field.name.description")).build(),
                nullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.description.identifier.description")).build()
        );
    }

    private GraphQLFieldDefinition.Builder descriptionsFieldDefinition(GraphQLOutputType descriptionType) {
        return newFieldDefinition()
                .type(new GraphQLList(descriptionType))
                .name("descriptions")
                .description(__("graphql.field.descriptions.description"))
                .dataFetcher(oneToManyRelationshipFetcher(r -> r.as(Described.class).getDescriptions()));
    }

    private GraphQLFieldDefinition.Builder itemCountFieldDefinition(Function<Entity, Integer> f) {
        return newFieldDefinition()
                .type(new GraphQLNonNull(GraphQLInt))
                .name("itemCount")
                .description(__("graphql.field.itemCount.description"))
                .dataFetcher(env -> Math.toIntExact(f.apply(env.<Entity>getSource())));
    }

    private final GraphQLFieldDefinition.Builder linkFieldDefinition =
            listFieldDefinition("links", __("graphql.field.links.description"),
                    new GraphQLTypeReference(Entities.LINK),
                    oneToManyRelationshipFetcher(r -> r.as(Linkable.class).getLinks()));

    private final GraphQLFieldDefinition.Builder annotationsFieldDefinition =
            listFieldDefinition("annotations", __("graphql.field.annotations.description"),
                    new GraphQLTypeReference(Entities.ANNOTATION),
                    oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations()));

    private List<GraphQLFieldDefinition> linksAndAnnotationsFields() {
        return Lists.newArrayList(linkFieldDefinition.build(), annotationsFieldDefinition.build());
    }

    private final GraphQLFieldDefinition.Builder accessPointFieldDefinition =
        listFieldDefinition("accessPoints", __("graphql.field.accessPoints.description"),
                new GraphQLTypeReference(Entities.ACCESS_POINT),
                oneToManyRelationshipFetcher(d -> d.as(Description.class).getAccessPoints()));

    private final GraphQLFieldDefinition.Builder datePeriodFieldDefinition =
        listFieldDefinition("dates", __("graphql.field.dates.description"),
                new GraphQLTypeReference(Entities.DATE_PERIOD),
                oneToManyRelationshipFetcher(d -> d.as(Temporal.class).getDatePeriods()));

    private <T> GraphQLFieldDefinition.Builder itemFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher<T> dataFetcher, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .type(type)
                .description(description)
                .dataFetcher(dataFetcher)
                .argument(Lists.newArrayList(arguments));
    }

    private GraphQLFieldDefinition relatedTypeFieldDefinition() {
        return newFieldDefinition()
                .name("related")
                .description(__("graphql.field.related.description"))
                .type(new GraphQLList(relatedType))
                .dataFetcher(relatedItemsDataFetcher)
                .build();
    }

    private GraphQLFieldDefinition relatedItemsItemFieldDefinition() {
        return newFieldDefinition()
                .type(linkableInterface)
                .name("item")
                .description(__("graphql.field.related.item.description"))
                .build();
    }

    // Type definitions...

    private static GraphQLOutputType edgeType(GraphQLOutputType wrapped) {
        return newObject()
                .name(wrapped.getName() + "Edge")
                .description(MessageFormat.format(__("graphql.edge.description"), wrapped.getName()))
                .field(newFieldDefinition()
                        .name(NODE)
                        .type(wrapped)
                        .build()
                )
                .field(newFieldDefinition()
                        .name(CURSOR)
                        .type(CursorType)
                        .build()
                )
                .build();
    }

    private static List<GraphQLFieldDefinition> connectionFields(GraphQLOutputType wrappedType) {
        return ImmutableList.of(
                newFieldDefinition()
                        .name(ITEMS)
                        .description(MessageFormat.format(__("graphql.connection.field.items.description"), wrappedType.getName()))
                        .type(new GraphQLList(wrappedType))
                        .build(),
                newFieldDefinition()
                        .name(EDGES)
                        .description(MessageFormat.format(__("graphql.connection.field.edges.description"), wrappedType.getName()))
                        .type(new GraphQLList(edgeType(wrappedType)))
                        .build(),
                newFieldDefinition()
                        .name(PAGE_INFO)
                        .description(__("graphql.field.pageInfo.description"))
                        .type(newObject()
                                .name(PAGE_INFO + wrappedType.getName())
                                .field(newFieldDefinition()
                                        .name(HAS_PREVIOUS_PAGE)
                                        .description(__("graphql.field.pageInfo.hasPreviousPage.description"))
                                        .type(GraphQLBoolean)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(PREVIOUS_PAGE)
                                        .description(__("graphql.field.pageInfo.previousPage.description"))
                                        .type(CursorType)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(HAS_NEXT_PAGE)
                                        .description(__("graphql.field.pageInfo.hasNextPage.description"))
                                        .type(GraphQLBoolean)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(NEXT_PAGE)
                                        .description(__("graphql.field.pageInfo.nextPage.description"))
                                        .type(CursorType)
                                        .build()
                                )
                                .build())
                        .build()
        );
    }

    private static GraphQLOutputType connectionType(GraphQLOutputType wrappedType, String name) {
        return newObject()
                .name(name)
                .description(MessageFormat.format(__("graphql.connection.description"), wrappedType.getName()))
                .fields(connectionFields(wrappedType))
                .build();
    }

    // NB: These are static since loading them, and all the resources, is quite
    // slow to do per query.
    private static final List<GraphQLFieldDefinition> documentaryUnitDescriptionNullFields = nullStringAttrs(IsadG.values());
    private static final List<GraphQLFieldDefinition> documentaryUnitDescriptionListFields = listStringAttrs(IsadG.values());
    private static final List<GraphQLFieldDefinition> repositoryDescriptionNullFields = nullStringAttrs(Isdiah.values());
    private static final List<GraphQLFieldDefinition> repositoryDescriptionListFields = listStringAttrs(Isdiah.values());
    private static final List<GraphQLFieldDefinition> historicalAgentDescriptionNullFields = nullStringAttrs(Isaar.values());
    private static final List<GraphQLFieldDefinition> historicalAgentDescriptionListFields = listStringAttrs(Isaar.values());
    private static final List<GraphQLFieldDefinition> countryDescriptionNullFields = nullStringAttrs(CountryInfo.values());
    private static final List<GraphQLFieldDefinition> countryDescriptionListFields = listStringAttrs(CountryInfo.values());
    private static final List<GraphQLFieldDefinition> conceptNullFields = nullStringAttrs(Skos.values());
    private static final List<GraphQLFieldDefinition> conceptListFields = listStringAttrs(Skos.values());
    private static final List<GraphQLFieldDefinition> conceptDescriptionNullFields = nullStringAttrs(SkosMultilingual.values());
    private static final List<GraphQLFieldDefinition> conceptDescriptionListFields = listStringAttrs(SkosMultilingual.values());


    // Interfaces and type resolvers...

    private final TypeResolver entityTypeResolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            Entity entity = env.getObject();
            switch (entity.getType()) {
                case Entities.DOCUMENTARY_UNIT:
                    return documentaryUnitType;
                case Entities.REPOSITORY:
                    return repositoryType;
                case Entities.COUNTRY:
                    return countryType;
                case Entities.HISTORICAL_AGENT:
                    return historicalAgentType;
                case Entities.CVOC_CONCEPT:
                    return conceptType;
                case Entities.CVOC_VOCABULARY:
                    return vocabularyType;
                case Entities.AUTHORITATIVE_SET:
                    return authoritativeSetType;
                case Entities.ANNOTATION:
                    return annotationType;
                case Entities.LINK:
                    return linkType;
                case Entities.ACCESS_POINT:
                    return accessPointType;
                case Entities.DATE_PERIOD:
                    return datePeriodType;
                default:
                    return null;
            }
        }
    };

    private final TypeResolver descriptionTypeResolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            Entity entity = env.getObject();
            switch (entity.getType()) {
                case Entities.DOCUMENTARY_UNIT_DESCRIPTION:
                    return documentaryUnitDescriptionType;
                case Entities.REPOSITORY_DESCRIPTION:
                    return repositoryDescriptionType;
                case Entities.HISTORICAL_AGENT_DESCRIPTION:
                    return historicalAgentDescriptionType;
                case Entities.CVOC_CONCEPT_DESCRIPTION:
                    return conceptDescriptionType;
                default:
                    return null;
            }
        }
    };

    private final GraphQLInterfaceType entityInterface = newInterface()
            .name(Entity.class.getSimpleName())
            .description(__("graphql.interface.entity.description"))
            .fields(entityFields)
            .typeResolver(entityTypeResolver)
            .build();

    private final GraphQLInterfaceType descriptionInterface = newInterface()
            .name(Description.class.getSimpleName())
            .description(__("graphql.interface.description.description"))
            .fields(descriptionFields())
            .typeResolver(descriptionTypeResolver)
            .build();

    private final GraphQLInterfaceType temporalDescriptionInterface = newInterface()
            .name(Temporal.class.getSimpleName() + Description.class.getSimpleName())
            .description(__("graphql.interface.temporalDescription.description"))
            .fields(descriptionFields())
            .field((f) -> datePeriodFieldDefinition)
            .typeResolver(descriptionTypeResolver)
            .build();

    private final GraphQLInterfaceType describedInterface = newInterface()
            .name(Described.class.getSimpleName())
            .description(__("graphql.interface.described.description"))
            .fields(entityFields)
            .field(singleDescriptionFieldDefinition(descriptionInterface))
            .field(descriptionsFieldDefinition(descriptionInterface))
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.identifier.description")))
            .fields(linksAndAnnotationsFields())
            .typeResolver(entityTypeResolver)
            .build();

    private final GraphQLInterfaceType temporalInterface = newInterface()
            .name(Temporal.class.getSimpleName())
            .description(__("graphql.interface.temporal.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.identifier.description")))
            .field(singleDescriptionFieldDefinition(temporalDescriptionInterface))
            .field(descriptionsFieldDefinition(temporalDescriptionInterface))
            .typeResolver(entityTypeResolver)
            .build();

    private final GraphQLInterfaceType annotatableInterface = newInterface()
            .fields(entityFields)
            .field(annotationsFieldDefinition)
            .typeResolver(entityTypeResolver)
            .name(Annotatable.class.getSimpleName())
            .description(__("graphql.interface.annotatable.description"))
            .build();

    private final GraphQLInterfaceType linkableInterface = newInterface()
            .fields(entityFields)
            .field(linkFieldDefinition)
            .typeResolver(entityTypeResolver)
            .name(Linkable.class.getSimpleName())
            .description(__("graphql.interface.linkable.description"))
            .build();

    private final GraphQLObjectType systemEventType = newObject()
            .name(Entities.SYSTEM_EVENT)
            .description(__("systemEvent.description"))
            .field(nonNullAttr(Ontology.EVENT_TIMESTAMP, bundle.getString("systemEvent.field.timestamp.description")))
            .field(nullAttr(Ontology.EVENT_LOG_MESSAGE, bundle.getString("systemEvent.field.logMessage.description")))
            .field(nonNullAttr(Ontology.EVENT_TYPE, bundle.getString("systemEvent.field.eventType.description")))
            .build();

    private final GraphQLEnumType accessPointTypeEnum = newEnum()
            .name(AccessPointType.class.getSimpleName())
            .description(__("graphql.enum.accessPointType.description"))
            .value(AccessPointType.person.name())
            .value(AccessPointType.family.name())
            .value(AccessPointType.corporateBody.name())
            .value(AccessPointType.subject.name())
            .value(AccessPointType.creator.name())
            .value(AccessPointType.place.name())
            .value(AccessPointType.genre.name())
            .build();

    private final GraphQLObjectType accessPointType = newObject()
            .name(Entities.ACCESS_POINT)
            .description(__("accessPoint.description"))
            .field(nonNullAttr(Ontology.NAME_KEY, __("accessPoint.field.name.description")))
            .field(newFieldDefinition()
                    .name(Ontology.ACCESS_POINT_TYPE)
                    .description(__("accessPoint.field.type.description"))
                    .type(new GraphQLNonNull(accessPointTypeEnum))
                    .dataFetcher(attributeDataFetcher)
                    .build())
            .build();

    private final GraphQLObjectType datePeriodType = newObject()
            .name(Entities.DATE_PERIOD)
            .description(__("datePeriod.description"))
            .field(nullAttr(Ontology.DATE_PERIOD_START_DATE, __("datePeriod.field.startDate.description")))
            .field(nullAttr(Ontology.DATE_PERIOD_END_DATE, __("datePeriod.field.endDate.description")))
            .build();

    private final GraphQLObjectType addressType = newObject()
            .name(Entities.ADDRESS)
            .description(__("address.description"))
            .fields(nullStringAttrs(ContactInfo.values()))
            .fields(listStringAttrs(ContactInfo.values()))
            .build();

    private final GraphQLObjectType relatedType = newObject()
            .name("Relationship")
            .description(__("relationship.description"))
            .field(newFieldDefinition()
                    .name("context")
                    .description(__("relationship.field.context.description"))
                    .type(new GraphQLTypeReference(Entities.LINK))
                    .build())
            .field(relatedItemsItemFieldDefinition())
            .build();


    private final GraphQLObjectType documentaryUnitDescriptionType = newObject()
            .name(Entities.DOCUMENTARY_UNIT_DESCRIPTION)
            .description(__("documentaryUnitDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(datePeriodFieldDefinition)
            .fields(documentaryUnitDescriptionNullFields)
            .fields(documentaryUnitDescriptionListFields)
            .withInterfaces(descriptionInterface, temporalDescriptionInterface)
            .build();

    private final GraphQLObjectType repositoryDescriptionType = newObject()
            .name(Entities.REPOSITORY_DESCRIPTION)
            .description(__("repositoryDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(listFieldDefinition("addresses", __("repositoryDescription.field.addresses.description"),
                    addressType, oneToManyRelationshipFetcher(d ->
                            d.as(RepositoryDescription.class).getAddresses())))
            .fields(repositoryDescriptionNullFields)
            .fields(repositoryDescriptionListFields)
            .withInterfaces(descriptionInterface)
            .build();

    private final GraphQLObjectType historicalAgentDescriptionType = newObject()
            .name(Entities.HISTORICAL_AGENT_DESCRIPTION)
            .description(__("historicalAgentDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(datePeriodFieldDefinition)
            .fields(historicalAgentDescriptionNullFields)
            .fields(historicalAgentDescriptionListFields)
            .withInterfaces(descriptionInterface, temporalDescriptionInterface)
            .build();

    private final GraphQLObjectType conceptDescriptionType = newObject()
            .name(Entities.CVOC_CONCEPT_DESCRIPTION)
            .description(__("conceptDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .fields(conceptDescriptionNullFields)
            .fields(conceptDescriptionListFields)
            .withInterfaces(descriptionInterface)
            .build();

    private static final GraphQLArgument allArgument = newArgument()
            .name(ALL_PARAM)
            .description(__("graphql.argument.all"))
            .type(GraphQLBoolean)
            .defaultValue(false)
            .build();

    private static final GraphQLArgument topLevelArgument = newArgument()
            .name(TOP_LEVEL_PARAM)
            .description(__("graphql.argument.topLevel"))
            .type(GraphQLBoolean)
            .defaultValue(false)
            .build();

    private final GraphQLObjectType repositoryType = newObject()
            .name(Entities.REPOSITORY)
            .description(__("repository.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("repository.field.identifier.description")))
            .field(itemCountFieldDefinition(r -> r.as(Repository.class).countChildren()))
            .field(connectionFieldDefinition("documentaryUnits", __("repository.field.documentaryUnits.description"),
                    new GraphQLTypeReference("documentaryUnits"),
                    hierarchicalOneToManyRelationshipConnectionFetcher(
                            r -> r.as(Repository.class).getTopLevelDocumentaryUnits(),
                            r -> r.as(Repository.class).getAllDocumentaryUnits()),
                    allArgument))
            .fields(geoFields)
            .field(singleDescriptionFieldDefinition(repositoryDescriptionType))
            .field(descriptionsFieldDefinition(repositoryDescriptionType))
            .field(itemFieldDefinition("country", __("repository.field.country.description"),
                    new GraphQLTypeReference(Entities.COUNTRY),
                    manyToOneRelationshipFetcher(r -> r.as(Repository.class).getCountry())))
            .fields(linksAndAnnotationsFields())
            .field(relatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType documentaryUnitType = newObject()
            .name(Entities.DOCUMENTARY_UNIT)
            .description(__("documentaryUnit.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("documentaryUnit.field.identifier.description")))
            .field(descriptionsFieldDefinition(documentaryUnitDescriptionType))
            .field(singleDescriptionFieldDefinition(documentaryUnitDescriptionType))
            .field(itemFieldDefinition("repository", __("documentaryUnit.field.repository.description"), repositoryType,
                    manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getRepository())))
            .field(itemCountFieldDefinition(d -> d.as(DocumentaryUnit.class).countChildren()))
            .field(connectionFieldDefinition("children", __("documentaryUnit.field.children.description"),
                    new GraphQLTypeReference("documentaryUnits"),
                    hierarchicalOneToManyRelationshipConnectionFetcher(
                            d -> d.as(DocumentaryUnit.class).getChildren(),
                            d -> d.as(DocumentaryUnit.class).getAllChildren()),
                    allArgument))
            .field(itemFieldDefinition("parent", __("documentaryUnit.field.parent.description"),
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getParent())))
            .field(listFieldDefinition("ancestors", __("documentaryUnit.field.ancestors.description"),
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    oneToManyRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getAncestors())))
            .fields(linksAndAnnotationsFields())
            .field(relatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface, temporalInterface)
            .build();

    private final GraphQLObjectType historicalAgentType = newObject()
            .name(Entities.HISTORICAL_AGENT)
            .description(__("historicalAgent.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("historicalAgent.field.identifier.description")))
            .field(singleDescriptionFieldDefinition(historicalAgentDescriptionType))
            .field(descriptionsFieldDefinition(historicalAgentDescriptionType))
            .fields(linksAndAnnotationsFields())
            .field(relatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface, temporalInterface)
            .build();

    private final GraphQLObjectType authoritativeSetType = newObject()
            .name(Entities.AUTHORITATIVE_SET)
            .description(__("authoritativeSet.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("authoritativeSet.field.identifier.description")))
            .field(nonNullAttr(Ontology.NAME_KEY, __("authoritativeSet.field.name.description")))
            .field(nullAttr("description", __("authoritativeSet.field.description.description")))
            .field(itemCountFieldDefinition(a -> a.as(AuthoritativeSet.class).countChildren()))
            .field(connectionFieldDefinition("authorities", __("authoritativeSet.field.authorities.description"),
                    new GraphQLTypeReference("historicalAgents"),
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(AuthoritativeSet.class).getAuthoritativeItems())))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType countryType = newObject()
            .name(Entities.COUNTRY)
            .description(__("country.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("country.field.identifier.description")))
            .field(newFieldDefinition()
                    .name(Ontology.NAME_KEY)
                    .description(__("country.field.name.description"))
                    .type(new GraphQLNonNull(GraphQLString))
                    .dataFetcher(transformingDataFetcher(idDataFetcher,
                            obj -> LanguageHelpers.countryCodeToName(obj.toString())))
                    .build()
            )
            .fields(countryDescriptionNullFields)
            .fields(countryDescriptionListFields)
            .field(itemCountFieldDefinition(c -> c.as(Country.class).countChildren()))
            .field(connectionFieldDefinition("repositories", __("country.field.repositories.description"),
                    new GraphQLTypeReference("repositories"),
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(Country.class).getRepositories())))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType conceptType = newObject()
            .name(Entities.CVOC_CONCEPT)
            .description(__("cvocConcept.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("cvocConcept.field.identifier.description")))
            .fields(conceptNullFields)
            .fields(conceptListFields)
            .fields(geoFields)
            .field(descriptionsFieldDefinition(conceptDescriptionType))
            .field(singleDescriptionFieldDefinition(conceptDescriptionType))
            .field(listFieldDefinition("related", __("cvocConcept.field.related.description"),
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(
                            c -> c.as(Concept.class).getRelatedConcepts())))
            .field(itemCountFieldDefinition(c -> c.as(Concept.class).countChildren()))
            .field(listFieldDefinition("broader", __("cvocConcept.field.broader.description"),
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(c -> c.as(Concept.class).getBroaderConcepts())))
            .field(listFieldDefinition("narrower", __("cvocConcept.field.narrower.description"),
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(
                            c -> c.as(Concept.class).getNarrowerConcepts())))
            .field(itemFieldDefinition("vocabulary", __("cvocConcept.field.vocabulary.description"),
                    new GraphQLTypeReference(Entities.CVOC_VOCABULARY),
                    manyToOneRelationshipFetcher(c -> c.as(Concept.class).getVocabulary())))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType vocabularyType = newObject()
            .name(Entities.CVOC_VOCABULARY)
            .description(__("cvocVocabulary.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("cvocVocabulary.field.identifier.description")))
            .field(nonNullAttr(Ontology.NAME_KEY, __("cvocVocabulary.field.name.description")))
            .field(nullAttr("description", __("cvocVocabulary.field.description.description")))
            .field(itemCountFieldDefinition(r -> r.as(Vocabulary.class).countChildren()))
            .field(connectionFieldDefinition("concepts", __("cvocVocabulary.field.concepts.description"),
                    new GraphQLTypeReference("concepts"),
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(Vocabulary.class).getConcepts())))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType annotationType = newObject()
            .name(Entities.ANNOTATION)
            .description(__("annotation.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.ANNOTATION_NOTES_BODY, __("annotation.field.body.description")))
            .field(nullAttr(Ontology.ANNOTATION_FIELD, __("annotation.field.field.description")))
            .field(nullAttr(Ontology.ANNOTATION_TYPE, __("annotation.field.annotationType.description")))
            .field(newFieldDefinition()
                    .type(GraphQLString)
                    .name("by")
                    .description(__("annotation.field.by.description"))
                    .dataFetcher(annotationNameDataFetcher)
                    .build()
            )
            .field(listFieldDefinition("targets", __("annotation.field.targets.description"),
                    annotatableInterface,
                    oneToManyRelationshipFetcher(a -> a.as(Annotation.class).getTargets())))
            .field(listFieldDefinition("annotations", __("annotation.field.annotations.description"),
                    new GraphQLTypeReference(Entities.ANNOTATION),
                    oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations())))
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    private final GraphQLObjectType linkType = newObject()
            .name(Entities.LINK)
            .description(__("link.description"))
            .fields(entityFields)
            .field(nullAttr(Ontology.LINK_HAS_DESCRIPTION, __("link.field.description.description")))
            .field(nullAttr(Ontology.LINK_HAS_FIELD, __("link.field.field.description")))
            .field(listFieldDefinition("targets", __("link.field.targets.description"), linkableInterface,
                    oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkTargets())))
            .field(listFieldDefinition("body", __("link.field.body.description"), accessPointType,
                    oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkBodies())))
            .field(annotationsFieldDefinition)
            .field(datePeriodFieldDefinition)
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    private final GraphQLOutputType documentaryUnitsConnection = connectionType(
            new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),"documentaryUnits");

    private final GraphQLOutputType repositoriesConnection = connectionType(
            new GraphQLTypeReference(Entities.REPOSITORY),"repositories");

    private final GraphQLOutputType countriesConnection = connectionType(
            new GraphQLTypeReference(Entities.COUNTRY),"countries");

    private final GraphQLOutputType historicalAgentsConnection = connectionType(
            new GraphQLTypeReference(Entities.HISTORICAL_AGENT),"historicalAgents");

    private final GraphQLOutputType authoritativeSetsConnection = connectionType(
            new GraphQLTypeReference(Entities.AUTHORITATIVE_SET),"authoritativeSets");

    private final GraphQLOutputType conceptsConnection = connectionType(
            new GraphQLTypeReference(Entities.CVOC_CONCEPT),"concepts");

    private final GraphQLOutputType vocabulariesConnection = connectionType(
            new GraphQLTypeReference(Entities.CVOC_VOCABULARY),"vocabularies");

    private final GraphQLOutputType annotationsConnection = connectionType(
            new GraphQLTypeReference(Entities.ANNOTATION),"annotations");

    private final GraphQLOutputType linksConnection = connectionType(
            new GraphQLTypeReference(Entities.LINK),"links");

    private GraphQLObjectType queryType() {
        return newObject()
                .name("Root")

                // Single item types...
                .field(itemFieldDefinition(Entities.DOCUMENTARY_UNIT, __("root.single.documentaryUnit.description"),
                        documentaryUnitType, entityIdDataFetcher(Entities.DOCUMENTARY_UNIT), idArgument))
                .field(itemFieldDefinition(Entities.REPOSITORY, __("root.single.repository.description"),
                        repositoryType, entityIdDataFetcher(Entities.REPOSITORY), idArgument))
                .field(itemFieldDefinition(Entities.COUNTRY, __("root.single.country.description"),
                        countryType, entityIdDataFetcher(Entities.COUNTRY), idArgument))
                .field(itemFieldDefinition(Entities.HISTORICAL_AGENT, __("root.single.historicalAgent.description"),
                        historicalAgentType, entityIdDataFetcher(Entities.HISTORICAL_AGENT), idArgument))
                .field(itemFieldDefinition(Entities.AUTHORITATIVE_SET, __("root.single.authoritativeSet.description"),
                        authoritativeSetType, entityIdDataFetcher(Entities.AUTHORITATIVE_SET), idArgument))
                .field(itemFieldDefinition(Entities.CVOC_CONCEPT, __("root.single.cvocConcept.description"),
                        conceptType, entityIdDataFetcher(Entities.CVOC_CONCEPT), idArgument))
                .field(itemFieldDefinition(Entities.CVOC_VOCABULARY, __("root.single.cvocVocabulary.description"),
                        vocabularyType, entityIdDataFetcher(Entities.CVOC_VOCABULARY), idArgument))
                .field(itemFieldDefinition(Entities.ANNOTATION, __("root.single.annotation.description"),
                        annotationType, entityIdDataFetcher(Entities.ANNOTATION), idArgument))
                .field(itemFieldDefinition(Entities.LINK, __("root.single.link.description"),
                        linkType, entityIdDataFetcher(Entities.LINK), idArgument))

                // Top level item connections
                .field(connectionFieldDefinition("documentaryUnits", __("root.connection.documentaryUnit.description"),
                        documentaryUnitsConnection,
                        docDataFetcher(), topLevelArgument))
                .field(connectionFieldDefinition("topLevelDocumentaryUnits", __("root.connection.documentaryUnit.topLevel.description"),
                        documentaryUnitsConnection,
                        topLevelDocDataFetcher())
                            .deprecate(__("root.connection.documentaryUnit.topLevel.deprecationReason")))
                .field(connectionFieldDefinition("repositories", __("root.connection.repository.description"),
                        repositoriesConnection,
                        entityTypeConnectionDataFetcher(EntityClass.REPOSITORY)))
                .field(connectionFieldDefinition("historicalAgents", __("root.connection.historicalAgent.description"),
                        historicalAgentsConnection,
                        entityTypeConnectionDataFetcher(EntityClass.HISTORICAL_AGENT)))
                .field(connectionFieldDefinition("countries", __("root.connection.country.description"),
                        countriesConnection,
                        entityTypeConnectionDataFetcher(EntityClass.COUNTRY)))
                .field(connectionFieldDefinition("authoritativeSets", __("root.connection.authoritativeSet.description"),
                        authoritativeSetsConnection,
                        entityTypeConnectionDataFetcher(EntityClass.AUTHORITATIVE_SET)))
                .field(connectionFieldDefinition("concepts", __("root.connection.cvocConcept.description"),
                        conceptsConnection,
                        entityTypeConnectionDataFetcher(EntityClass.CVOC_CONCEPT)))
                .field(connectionFieldDefinition("vocabularies", __("root.connection.cvocVocabulary.description"),
                        vocabulariesConnection,
                        entityTypeConnectionDataFetcher(EntityClass.CVOC_VOCABULARY)))
                .field(connectionFieldDefinition("annotations", __("root.connection.annotation.description"),
                        annotationsConnection,
                        entityTypeConnectionDataFetcher(EntityClass.ANNOTATION)))
                .field(connectionFieldDefinition("links", __("root.connection.link.description"),
                        linksConnection,
                        entityTypeConnectionDataFetcher(EntityClass.LINK)))
                .build();
    }
}
