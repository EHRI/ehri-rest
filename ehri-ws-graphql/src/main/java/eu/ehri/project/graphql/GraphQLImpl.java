/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.ContactInfo;
import eu.ehri.project.definitions.CountryInfo;
import eu.ehri.project.definitions.DefinitionList;
import eu.ehri.project.definitions.Entities;
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
import eu.ehri.project.models.base.Temporal;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.LanguageHelpers;
import graphql.relay.Base64;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Implementation of a GraphQL schema over the API
 */
public class GraphQLImpl {

    private static final String SLICE_PARAM = "at";
    private static final String FIRST_PARAM = "first";
    private static final String FROM_PARAM = "from";
    private static final String AFTER_PARAM = "after";
    private static final String ALL_PARAM = "all";

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

    private final Api _api;
    private final boolean stream;

    public GraphQLImpl(Api api, boolean stream) {
        this._api = api;
        this.stream = stream;
    }

    public GraphQLImpl(Api api) {
        this(api, false);
    }

    private Api api() {
        return _api;
    }

    public GraphQLSchema getSchema() {
        return GraphQLSchema.newSchema()
                .query(queryType())
                .build();
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
                    ? Math.max(-1, Integer.parseInt(Base64.fromBase64(cursor)))
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

    private static GraphQLScalarType wrappedString(String name, String description) {
        return new GraphQLScalarType(name, description, GraphQLString.getCoercing());
    }

    private static final GraphQLScalarType IdType = wrappedString("Id", "An entity global string identifier");
    private static final GraphQLScalarType CursorType = wrappedString("Cursor", "A connection cursor");

    private static final GraphQLArgument idArgument = newArgument()
            .name(Bundle.ID_KEY)
            .type(new GraphQLNonNull(IdType))
            .build();

    // Data fetchers...

    private DataFetcher topLevelDocDataFetcher() {
        Iterable<Country> countries = api().query()
                .setStream(true).setLimit(-1).page(EntityClass.COUNTRY, Country.class);
        Iterable<Iterable<Entity>> docs = Iterables.transform(countries, c ->
                Iterables.transform(c.getTopLevelDocumentaryUnits(), d -> d.as(Entity.class)));
        return connectionDataFetcher(() -> Iterables.concat(docs));
    }

    private DataFetcher entityTypeConnectionDataFetcher(EntityClass type) {
        // FIXME: The only way to get a list of all items of a given
        // type via the API alone is to run a query as a stream w/ no limit.
        // However, this means that ACL filtering will be applied twice,
        // once here, and once by the connection data fetcher, which also
        // applies pagination. This is a bit gross but the speed difference
        // appears to be negligible.
        return connectionDataFetcher(() -> api().query()
                .setStream(true).setLimit(-1).page(type, Entity.class));
    }

    private DataFetcher hierarchicalOneToManyRelationshipConnectionFetcher(
            Function<Entity, Iterable<? extends Entity>> top, Function<Entity, Iterable<? extends Entity>> all) {
        // Depending on the value of the "all" argument, return either just
        // the top level items or everything in the tree.
        return env -> {
            boolean allOrTop = (Boolean) Optional.ofNullable(env.getArgument(ALL_PARAM)).orElse(false);
            Function<Entity, Iterable<? extends Entity>> func = allOrTop ? all : top;
            return connectionDataFetcher(() -> func.apply(((Entity) env.getSource()))).get(env);
        };
    }

    private DataFetcher oneToManyRelationshipConnectionFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return env -> connectionDataFetcher(() -> f.apply(((Entity) env.getSource()))).get(env);
    }

    private DataFetcher connectionDataFetcher(Supplier<Iterable<? extends Entity>> iter) {
        // NB: The data fetcher takes a supplier here so lazily generated
        // streams can be invoked more than one (if, e.g. both the items array
        // and the edges array is needed.) Otherwise we would have to somehow
        // reset the Iterable.
        return env -> {
            int limit = getLimit(env.getArgument(FIRST_PARAM), stream);
            int offset = getOffset(env.getArgument(AFTER_PARAM), env.getArgument(FROM_PARAM));
            return stream && limit < 0
                    ? lazyConnectionDataFetcher(iter, limit, offset)
                    : strictConnectionDataFetcher(iter, limit, offset);
        };
    }

    private Map<String, Object> connectionData(Iterable<Entity> items,
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

    private Map<String, Object> strictConnectionDataFetcher(Supplier<Iterable<? extends Entity>> iter, int limit, int offset) {
        QueryApi query = api().query().setLimit(limit).setOffset(offset);
        QueryApi.Page<Entity> page = query.page(iter.get(), Entity.class);
        List<Entity> items = Lists.newArrayList(page);

        // Create a list of edges, with the cursor taking into
        // account each item's offset
        List<Map<String, Object>> edges = Lists.newArrayListWithExpectedSize(items.size());
        for (int i = 0; i < items.size(); i++) {
            edges.add(mapOf(
                    CURSOR, Base64.toBase64(String.valueOf(offset + i)),
                    NODE, items.get(i)
            ));
        }

        boolean hasNext = page.getOffset() + items.size() < page.getTotal();
        boolean hasPrev = page.getOffset() > 0;
        String nextCursor = Base64.toBase64(String.valueOf(offset + limit));
        String prevCursor = Base64.toBase64(String.valueOf(offset - limit));
        return connectionData(items, edges, hasNext ? nextCursor : null, hasPrev ? prevCursor : null);
    }

    private Map<String, Object> lazyConnectionDataFetcher(Supplier<Iterable<? extends Entity>> iter, int limit, int offset) {
        QueryApi query = api().query().setLimit(limit).setOffset(offset);
        QueryApi.Page<Entity> items = query.page(iter.get(), Entity.class);
        boolean hasPrev = items.getOffset() > 0;

        // Create a list of edges, with the cursor taking into
        // account each item's offset
        final AtomicInteger index = new AtomicInteger();
        Iterable<Map<String, Object>> edges = Iterables.transform(
                query.page(iter.get(), Entity.class), item -> mapOf(
                        CURSOR, Base64.toBase64(String.valueOf(offset + index.getAndIncrement())),
                        NODE, item
                ));

        String prevCursor = Base64.toBase64(String.valueOf(offset - limit));
        return connectionData(items, edges, null, hasPrev ? prevCursor : null);
    }

    private DataFetcher entityIdDataFetcher(String type) {
        return env -> {
            try {
                Accessible detail = api().detail(env.getArgument(Bundle.ID_KEY), Accessible.class);
                return Objects.equals(detail.getType(), type) ? detail : null;
            } catch (ItemNotFound e) {
                return null;
            }
        };
    }

    ;

    private static final DataFetcher idDataFetcher =
            environment -> ((Entity) environment.getSource()).getProperty(EntityType.ID_KEY);

    private static final DataFetcher typeDataFetcher =
            environment -> ((Entity) environment.getSource()).getProperty(EntityType.TYPE_KEY);

    private static final DataFetcher attributeDataFetcher =
            environment -> ((Entity) environment.getSource())
                    .getProperty(environment.getFields().get(0).getName());

    private static DataFetcher listDataFetcher(DataFetcher fetcher) {
        return environment -> {
            Object obj = fetcher.get(environment);
            if (obj == null) {
                return Collections.emptyList();
            } else if (obj instanceof List) {
                return obj;
            } else {
                return Lists.newArrayList(obj);
            }
        };
    }

    private static final DataFetcher descriptionDataFetcher = environment -> {
        String lang = environment.getArgument(Ontology.LANGUAGE_OF_DESCRIPTION);
        String code = environment.getArgument(Ontology.IDENTIFIER_KEY);

        Entity source = (Entity) environment.getSource();
        Iterable<Description> descriptions = source.as(Described.class).getDescriptions();

        if (lang == null && code == null) {
            int at = environment.getArgument(SLICE_PARAM);
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

    private static DataFetcher transformingDataFetcher(DataFetcher fetcher, Function<Object, Object> transformer) {
        return environment -> transformer.apply(fetcher.get(environment));
    }

    private DataFetcher oneToManyRelationshipFetcher(Function<Entity, Iterable<? extends Entity>> f) {
        return environment -> {
            Iterable<? extends Entity> elements = f.apply(((Entity) environment.getSource()));
            return api().query().setStream(true).setLimit(-1).page(elements, Entity.class);
        };
    }

    private DataFetcher manyToOneRelationshipFetcher(Function<Entity, Entity> f) {
        return environment -> {
            Entity elem = f.apply((Entity) environment.getSource());
            if (elem != null &&
                    AclManager.getAclFilterFunction(api().accessor())
                            .compute(elem.asVertex())) {
                return elem;
            }

            return null;
        };
    }

    // Field definition helpers...

    private static GraphQLFieldDefinition nullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description)
                .dataFetcher(attributeDataFetcher)
                .build();
    }

    private static GraphQLFieldDefinition nullAttr(String name, String description) {
        return nullAttr(name, description, GraphQLString);
    }

    private static GraphQLFieldDefinition nonNullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description)
                .dataFetcher(attributeDataFetcher)
                .build();
    }

    private static GraphQLFieldDefinition nonNullAttr(String name, String description) {
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

    private static final GraphQLFieldDefinition idField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name(Bundle.ID_KEY)
            .description("The item's EHRI id")
            .dataFetcher(idDataFetcher)
            .build();

    private static final GraphQLFieldDefinition typeField = newFieldDefinition()
            .type(new GraphQLNonNull(GraphQLString))
            .name(Bundle.TYPE_KEY)
            .description("The item's EHRI type")
            .dataFetcher(typeDataFetcher)
            .build();

    private static GraphQLFieldDefinition singleDescriptionFieldDefinition(GraphQLOutputType descriptionType) {
        return newFieldDefinition()
                .type(descriptionType)
                .name("description")
                .argument(newArgument()
                        .name(Ontology.LANGUAGE_OF_DESCRIPTION)
                        .description("The description's language code")
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name(Ontology.IDENTIFIER_KEY)
                        .description("The description's identifier code")
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name(SLICE_PARAM)
                        .description("The description's 1-based index index (default: 1)")
                        .type(GraphQLInt)
                        .defaultValue(1)
                        .build()
                )
                .description("Fetch the description at given the given index, or that with the given " +
                        "languageCode and/or identifier code. Since the default index is 1, no arguments will return " +
                        "the first available description.")
                .dataFetcher(descriptionDataFetcher)
                .build();
    }

    private static GraphQLFieldDefinition listFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher dataFetcher) {
        return newFieldDefinition()
                .name(name)
                .type(new GraphQLList(type))
                .description(description)
                .dataFetcher(dataFetcher)
                .build();
    }

    private static GraphQLFieldDefinition connectionFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher dataFetcher, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .description(description)
                .type(connectionType(type, name, description))
                .dataFetcher(dataFetcher)
                .argument(newArgument()
                        .name(FIRST_PARAM)
                        .type(GraphQLInt)
                        .description("The number of items after the cursor")
                        .build()
                )
                .argument(newArgument()
                        .name(AFTER_PARAM)
                        .description("Fetch items after this cursor")
                        .type(CursorType)
                        .build()
                )
                .argument(newArgument()
                        .name(FROM_PARAM)
                        .description("Fetch items from this cursor")
                        .type(CursorType)
                        .build()
                )
                .argument(Lists.newArrayList(arguments))
                .build();
    }

    private static final List<GraphQLFieldDefinition> entityFields =
            ImmutableList.of(idField, typeField);

    private static final List<GraphQLFieldDefinition> geoFields = ImmutableList.of(
            newFieldDefinition()
                    .name(Geo.latitude.name())
                    .description(Geo.latitude.getDescription())
                    .type(GraphQLBigDecimal)
                    .build(),
            newFieldDefinition()
                    .name(Geo.longitude.name())
                    .description(Geo.longitude.getDescription())
                    .type(GraphQLBigDecimal)
                    .build()
    );

    private static List<GraphQLFieldDefinition> descriptionFields() {
        return Lists.newArrayList(
                nonNullAttr(Ontology.LANGUAGE_OF_DESCRIPTION, "The description's language code"),
                nonNullAttr(Ontology.NAME_KEY, "The description's title"),
                nullAttr(Ontology.IDENTIFIER_KEY, "The description's (optional) identifier")
        );
    }

    private GraphQLFieldDefinition descriptionsFieldDefinition(GraphQLOutputType descriptionType) {
        return newFieldDefinition()
                .type(new GraphQLList(descriptionType))
                .name("descriptions")
                .description("The item's descriptions")
                .dataFetcher(oneToManyRelationshipFetcher(r -> r.as(Described.class).getDescriptions()))
                .build();
    }

    private GraphQLFieldDefinition itemCountFieldDefinition(Function<Entity, Integer> f) {
        return newFieldDefinition()
                .type(new GraphQLNonNull(GraphQLInt))
                .name("itemCount")
                .description("The number of child items this item contains")
                .dataFetcher(env -> Math.toIntExact(f.apply((Entity) env.getSource())))
                .build();
    }

    private List<GraphQLFieldDefinition> linksAndAnnotationsFields() {
        return Lists.newArrayList(
                listFieldDefinition("links", "This item's links",
                        new GraphQLTypeReference(Entities.LINK),
                        oneToManyRelationshipFetcher(r -> r.as(Linkable.class).getLinks())),
                listFieldDefinition("annotations", "This item's annotations",
                        new GraphQLTypeReference(Entities.ANNOTATION),
                        oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations()))
        );
    }

    private GraphQLFieldDefinition accessPointFieldDefinition() {
        return listFieldDefinition("accessPoints", "Access points associated with this description",
                accessPointType, oneToManyRelationshipFetcher(d -> d.as(Description.class).getAccessPoints()));
    }

    private GraphQLFieldDefinition datePeriodFieldDefinition() {
        return listFieldDefinition("dates", "Date periods associated with this description",
                datePeriodType, oneToManyRelationshipFetcher(d -> d.as(Temporal.class).getDatePeriods()));
    }

    private GraphQLFieldDefinition itemFieldDefinition(String name, String description,
            GraphQLOutputType type, DataFetcher dataFetcher, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .type(type)
                .description(description)
                .dataFetcher(dataFetcher)
                .argument(Lists.newArrayList(arguments))
                .build();
    }

    // Type definitions...

    private static GraphQLOutputType edgeType(GraphQLOutputType wrapped, String description) {
        return newObject()
                .name(wrapped.getName() + "Edge")
                .description(description)
                .field(newFieldDefinition()
                        .name(NODE)
                        .type(wrapped)
                        .build()
                )
                .field(newFieldDefinition()
                        .name(CURSOR)
                        .type(GraphQLString)
                        .build()
                )
                .build();
    }

    private static GraphQLOutputType connectionType(GraphQLOutputType wrappedType, String name, String description) {
        return newObject()
                .name(name)
                .description(description)
                .field(newFieldDefinition()
                        .name(ITEMS)
                        .description("A sequence of type: " + wrappedType.getName())
                        .type(new GraphQLList(wrappedType))
                        .build()
                ).field(newFieldDefinition()
                        .name(EDGES)
                        .description("A sequence of " + wrappedType.getName() + " edges")
                        .type(new GraphQLList(edgeType(wrappedType, description)))
                        .build()
                ).field(newFieldDefinition()
                        .name(PAGE_INFO)
                        .description("Pagination information")
                        .type(newObject()
                                .name(PAGE_INFO)
                                .field(newFieldDefinition()
                                        .name(HAS_PREVIOUS_PAGE)
                                        .description("If a previous page of data is available")
                                        .type(GraphQLBoolean)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(PREVIOUS_PAGE)
                                        .description("A cursor pointing to the previous page of items")
                                        .type(GraphQLString)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(HAS_NEXT_PAGE)
                                        .description("If another page of data is available")
                                        .type(GraphQLBoolean)
                                        .build()
                                )
                                .field(newFieldDefinition()
                                        .name(NEXT_PAGE)
                                        .description("A cursor pointing to the next page of items")
                                        .type(GraphQLString)
                                        .build()
                                )
                                .build())
                        .build()
                )
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

    private TypeResolver entityTypeResolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(Object item) {
            Entity entity = (Entity) item;
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
                    return entityType;
            }
        }
    };

    private TypeResolver descriptionTypeResolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(Object item) {
            Entity entity = (Entity) item;
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

    private TypeResolver describedTypeResolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(Object item) {
            Entity entity = (Entity) item;
            switch (entity.getType()) {
                case Entities.DOCUMENTARY_UNIT:
                    return documentaryUnitType;
                case Entities.REPOSITORY:
                    return repositoryType;
                case Entities.HISTORICAL_AGENT:
                    return historicalAgentType;
                case Entities.CVOC_CONCEPT:
                    return conceptType;
                default:
                    return null;
            }
        }
    };


    private final GraphQLInterfaceType entityInterface = newInterface()
            .name("Entity")
            .description("An entity")
            .fields(entityFields)
            .typeResolver(entityTypeResolver)
            .build();

    private final GraphQLInterfaceType descriptionInterface = newInterface()
            .name("Description")
            .description("A language-specific item description")
            .fields(descriptionFields())
            .typeResolver(descriptionTypeResolver)
            .build();

    private final GraphQLInterfaceType describedInterface = newInterface()
            .name("Described")
            .description("An item with multi-lingual descriptions")
            .fields(entityFields)
            .field(singleDescriptionFieldDefinition(descriptionInterface))
            .field(descriptionsFieldDefinition(descriptionInterface))
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The item's local identifier"))
            .fields(linksAndAnnotationsFields())
            .typeResolver(describedTypeResolver)
            .build();


    private final GraphQLObjectType entityType = newObject()
            .name("Entity")
            .description("An EHRI entity")
            .fields(entityFields)
            .withInterface(entityInterface)
            .build();

    private final GraphQLEnumType accessPointTypeEnum = newEnum()
            .name(AccessPointType.class.getSimpleName())
            .description("Access point types")
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
            .description("An access point")
            .field(nonNullAttr(Ontology.NAME_KEY, "The access point's text"))
            .field(newFieldDefinition()
                    .name(Ontology.ACCESS_POINT_TYPE)
                    .description("The access point's type")
                    .type(new GraphQLNonNull(accessPointTypeEnum))
                    .dataFetcher(attributeDataFetcher)
                    .build())
            .build();

    private final GraphQLObjectType datePeriodType = newObject()
            .name(Entities.DATE_PERIOD)
            .description("A date period")
            .field(nullAttr(Ontology.DATE_PERIOD_START_DATE, "The start date"))
            .field(nullAttr(Ontology.DATE_PERIOD_END_DATE, "The end date"))
            .build();

    private final GraphQLObjectType addressType = newObject()
            .name(Entities.ADDRESS)
            .description("An address")
            .fields(nullStringAttrs(ContactInfo.values()))
            .fields(listStringAttrs(ContactInfo.values()))
            .build();

    private final GraphQLObjectType documentaryUnitDescriptionType = newObject()
            .name(Entities.DOCUMENTARY_UNIT_DESCRIPTION)
            .description("An archival description")
            .fields(descriptionFields())
            .field(accessPointFieldDefinition())
            .field(datePeriodFieldDefinition())
            .fields(documentaryUnitDescriptionNullFields)
            .fields(documentaryUnitDescriptionListFields)
            .withInterface(descriptionInterface)
            .build();

    private final GraphQLObjectType repositoryDescriptionType = newObject()
            .name(Entities.REPOSITORY_DESCRIPTION)
            .description("A repository description")
            .fields(descriptionFields())
            .field(accessPointFieldDefinition())
            .field(listFieldDefinition("addresses", "Addresses",
                    addressType, oneToManyRelationshipFetcher(d ->
                            d.as(RepositoryDescription.class).getAddresses())))
            .fields(repositoryDescriptionNullFields)
            .fields(repositoryDescriptionListFields)
            .withInterface(descriptionInterface)
            .build();

    private final GraphQLObjectType historicalAgentDescriptionType = newObject()
            .name(Entities.HISTORICAL_AGENT_DESCRIPTION)
            .description("An historical agent description")
            .fields(descriptionFields())
            .field(accessPointFieldDefinition())
            .field(datePeriodFieldDefinition())
            .fields(historicalAgentDescriptionNullFields)
            .fields(historicalAgentDescriptionListFields)
            .withInterface(descriptionInterface)
            .build();

    private final GraphQLObjectType conceptDescriptionType = newObject()
            .name(Entities.CVOC_CONCEPT_DESCRIPTION)
            .description("A concept description")
            .fields(descriptionFields())
            .field(accessPointFieldDefinition())
            .fields(conceptDescriptionNullFields)
            .fields(conceptDescriptionListFields)
            .withInterface(descriptionInterface)
            .build();

    private static final GraphQLArgument allArgument = newArgument()
            .name(ALL_PARAM)
            .description("Fetch all lower level items, not just those at the next level.")
            .type(GraphQLBoolean)
            .defaultValue(false)
            .build();

    private final GraphQLObjectType repositoryType = newObject()
            .name(Entities.REPOSITORY)
            .description("A repository / archival institution")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The repository's EHRI identifier"))
            .field(itemCountFieldDefinition(r -> r.as(Repository.class).getChildCount()))
            .field(connectionFieldDefinition("documentaryUnits", "The repository's top level documentary units",
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    hierarchicalOneToManyRelationshipConnectionFetcher(
                            r -> r.as(Repository.class).getTopLevelDocumentaryUnits(),
                            r -> r.as(Repository.class).getAllDocumentaryUnits()),
                    allArgument))
            .field(singleDescriptionFieldDefinition(repositoryDescriptionType))
            .field(descriptionsFieldDefinition(repositoryDescriptionType))
            .field(itemFieldDefinition("country", "The repository's country",
                    new GraphQLTypeReference(Entities.COUNTRY),
                    manyToOneRelationshipFetcher(r -> r.as(Repository.class).getCountry())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .withInterface(describedInterface)
            .build();

    private final GraphQLObjectType documentaryUnitType = newObject()
            .name(Entities.DOCUMENTARY_UNIT)
            .description("An archival unit")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The item's local identifier"))
            .field(descriptionsFieldDefinition(documentaryUnitDescriptionType))
            .field(singleDescriptionFieldDefinition(documentaryUnitDescriptionType))
            .field(itemFieldDefinition("repository", "The unit's repository, if top level", repositoryType,
                    manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getRepository())))
            .field(itemCountFieldDefinition(d -> d.as(DocumentaryUnit.class).getChildCount()))
            .field(connectionFieldDefinition("children", "The unit's child items",
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    hierarchicalOneToManyRelationshipConnectionFetcher(
                            d -> d.as(DocumentaryUnit.class).getChildren(),
                            d -> d.as(DocumentaryUnit.class).getAllChildren()),
                    allArgument))
            .field(itemFieldDefinition("parent", "The unit's parent item, if applicable",
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getParent())))
            .field(listFieldDefinition("ancestors", "The unit's parent items, as a list",
                    new GraphQLTypeReference(Entities.DOCUMENTARY_UNIT),
                    oneToManyRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getAncestors())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .withInterface(describedInterface)
            .build();

    private final GraphQLObjectType historicalAgentType = newObject()
            .name(Entities.HISTORICAL_AGENT)
            .description("An historical agent")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The historical agent's EHRI identifier"))
            .field(singleDescriptionFieldDefinition(historicalAgentDescriptionType))
            .field(descriptionsFieldDefinition(historicalAgentDescriptionType))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .withInterface(describedInterface)
            .build();

    private final GraphQLObjectType authoritativeSetType = newObject()
            .name(Entities.AUTHORITATIVE_SET)
            .description("An authority set")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The set's local identifier"))
            .field(nonNullAttr(Ontology.NAME_KEY, "The set's name"))
            .field(nullAttr("description", "The item's description"))
            .field(itemCountFieldDefinition(a -> a.as(AuthoritativeSet.class).getChildCount()))
            .field(connectionFieldDefinition("authorities", "Item's contained in this vocabulary", historicalAgentType,
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(AuthoritativeSet.class).getAuthoritativeItems())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .build();


    private final GraphQLObjectType countryType = newObject()
            .name(Entities.COUNTRY)
            .description("A country")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The country's ISO639-2 code"))
            .field(newFieldDefinition()
                    .name(Ontology.NAME_KEY)
                    .description("The country's English Name")
                    .type(new GraphQLNonNull(GraphQLString))
                    .dataFetcher(transformingDataFetcher(idDataFetcher,
                            obj -> LanguageHelpers.countryCodeToName(obj.toString())))
                    .build()
            )
            .fields(countryDescriptionNullFields)
            .fields(countryDescriptionListFields)
            .field(itemCountFieldDefinition(c -> c.as(Country.class).getChildCount()))
            .field(connectionFieldDefinition("repositories", "Repositories located in the country", repositoryType,
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(Country.class).getRepositories())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .build();

    private final GraphQLObjectType conceptType = newObject()
            .name(Entities.CVOC_CONCEPT)
            .description("A concept")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The concept's local identifier"))
            .fields(conceptNullFields)
            .fields(conceptListFields)
            .fields(geoFields)
            .field(descriptionsFieldDefinition(conceptDescriptionType))
            .field(singleDescriptionFieldDefinition(conceptDescriptionType))
            .field(listFieldDefinition("related", "Related concepts, as a list",
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(
                            c -> c.as(Concept.class).getRelatedConcepts())))
            .field(itemCountFieldDefinition(c -> c.as(Concept.class).getChildCount()))
            .field(listFieldDefinition("broader", "Broader concepts, as a list",
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(c -> c.as(Concept.class).getBroaderConcepts())))
            .field(listFieldDefinition("narrower", "Narrower concepts, as a list",
                    new GraphQLTypeReference(Entities.CVOC_CONCEPT),
                    oneToManyRelationshipFetcher(
                            c -> c.as(Concept.class).getNarrowerConcepts())))
            .field(itemFieldDefinition("vocabulary", "The vocabulary",
                    new GraphQLTypeReference(Entities.CVOC_VOCABULARY),
                    manyToOneRelationshipFetcher(c -> c.as(Concept.class).getVocabulary())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .withInterface(describedInterface)
            .build();

    private final GraphQLObjectType vocabularyType = newObject()
            .name(Entities.CVOC_VOCABULARY)
            .description("A vocabulary")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, "The vocabulary's local identifier"))
            .field(nonNullAttr(Ontology.NAME_KEY, "The vocabulary's name"))
            .field(nullAttr("description", "The item's description"))
            .field(itemCountFieldDefinition(r -> r.as(Vocabulary.class).getChildCount()))
            .field(connectionFieldDefinition("concepts", "Concepts contained in this vocabulary", conceptType,
                    oneToManyRelationshipConnectionFetcher(
                            c -> c.as(Vocabulary.class).getConcepts())))
            .fields(linksAndAnnotationsFields())
            .withInterface(entityInterface)
            .build();

    private final GraphQLObjectType annotationType = newObject()
            .name(Entities.ANNOTATION)
            .description("An annotation")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.ANNOTATION_NOTES_BODY, "The text of the annotation"))
            .field(listFieldDefinition("targets", "The annotation's target(s)", entityType,
                    oneToManyRelationshipFetcher(a -> a.as(Annotation.class).getTargets())))
            .field(listFieldDefinition("annotations", "This item's annotations",
                    new GraphQLTypeReference(Entities.ANNOTATION),
                    oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations())))
            .withInterface(entityInterface)
            .build();

    private final GraphQLObjectType linkType = newObject()
            .name(Entities.LINK)
            .description("A link")
            .fields(entityFields)
            .field(nonNullAttr(Ontology.LINK_HAS_DESCRIPTION, "The link description"))
            .field(listFieldDefinition("targets", "The annotation's targets", entityType,
                    oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkTargets())))
            .field(listFieldDefinition("body", "The links's body(s)", accessPointType,
                    oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkBodies())))
            .field(listFieldDefinition("annotations", "This item's annotations",
                    new GraphQLTypeReference(Entities.ANNOTATION),
                    oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations())))
            .withInterface(entityInterface)
            .build();

    private GraphQLObjectType queryType() {
        return newObject()
                .name("Root")

                // Single item types...
                .field(itemFieldDefinition(Entities.DOCUMENTARY_UNIT, "Fetch a single documentary unit",
                        documentaryUnitType, entityIdDataFetcher(Entities.DOCUMENTARY_UNIT), idArgument))
                .field(itemFieldDefinition(Entities.REPOSITORY, "Fetch a single repository",
                        repositoryType, entityIdDataFetcher(Entities.REPOSITORY), idArgument))
                .field(itemFieldDefinition(Entities.COUNTRY, "Fetch a single country",
                        countryType, entityIdDataFetcher(Entities.COUNTRY), idArgument))
                .field(itemFieldDefinition(Entities.HISTORICAL_AGENT, "Fetch a single historical agent",
                        historicalAgentType, entityIdDataFetcher(Entities.HISTORICAL_AGENT), idArgument))
                .field(itemFieldDefinition(Entities.AUTHORITATIVE_SET, "Fetch a single authority set",
                        authoritativeSetType, entityIdDataFetcher(Entities.AUTHORITATIVE_SET), idArgument))
                .field(itemFieldDefinition(Entities.CVOC_CONCEPT, "Fetch a single concept",
                        conceptType, entityIdDataFetcher(Entities.CVOC_CONCEPT), idArgument))
                .field(itemFieldDefinition(Entities.CVOC_VOCABULARY, "Fetch a single vocabulary",
                        vocabularyType, entityIdDataFetcher(Entities.CVOC_VOCABULARY), idArgument))
                .field(itemFieldDefinition(Entities.ANNOTATION, "Fetch a single annotation",
                        annotationType, entityIdDataFetcher(Entities.ANNOTATION), idArgument))
                .field(itemFieldDefinition(Entities.LINK, "Fetch a single link",
                        linkType, entityIdDataFetcher(Entities.LINK), idArgument))

                .field(connectionFieldDefinition("documentaryUnits", "A page of documentary units",
                        documentaryUnitType,
                        entityTypeConnectionDataFetcher(EntityClass.DOCUMENTARY_UNIT)))
                .field(connectionFieldDefinition("topLevelDocumentaryUnits", "A page of top level documentary units",
                        documentaryUnitType,
                        topLevelDocDataFetcher()))
                .field(connectionFieldDefinition("repositories", "A page of repositories",
                        repositoryType,
                        entityTypeConnectionDataFetcher(EntityClass.REPOSITORY)))
                .field(connectionFieldDefinition("historicalAgents", "A page of historical agents",
                        historicalAgentType,
                        entityTypeConnectionDataFetcher(EntityClass.HISTORICAL_AGENT)))
                .field(connectionFieldDefinition("countries", "A page of countries",
                        countryType,
                        entityTypeConnectionDataFetcher(EntityClass.COUNTRY)))
                .field(connectionFieldDefinition("authoritativeSets", "A page of authoritative sets",
                        authoritativeSetType,
                        entityTypeConnectionDataFetcher(EntityClass.AUTHORITATIVE_SET)))
                .field(connectionFieldDefinition("concepts", "A page of concepts",
                        conceptType,
                        entityTypeConnectionDataFetcher(EntityClass.CVOC_CONCEPT)))
                .field(connectionFieldDefinition("vocabularies", "A page of vocabularies",
                        vocabularyType,
                        entityTypeConnectionDataFetcher(EntityClass.CVOC_VOCABULARY)))
                .field(connectionFieldDefinition("annotations", "A page of annotation",
                        annotationType,
                        entityTypeConnectionDataFetcher(EntityClass.ANNOTATION)))
                .field(connectionFieldDefinition("links", "A page of links",
                        linkType,
                        entityTypeConnectionDataFetcher(EntityClass.LINK)))

                .build();
    }
}
