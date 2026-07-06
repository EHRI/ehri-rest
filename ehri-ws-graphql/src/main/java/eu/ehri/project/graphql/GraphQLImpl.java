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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.CountryInfo;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.LanguageHelpers;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.List;

import static eu.ehri.project.graphql.DataFetchers.*;
import static eu.ehri.project.graphql.FieldDefinitions.*;
import static eu.ehri.project.graphql.Messages.__;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Implementation of a GraphQL schema over the API.
 * <p>
 * The schema's shape (types, interfaces, enums) lives in {@link SchemaTypes},
 * and the logic that resolves fields against the API lives in
 * {@link DataFetchers}. This class wires the two together: {@link #codeRegistry()}
 * attaches a data fetcher to each schema field, and {@link #queryType()} defines
 * the root query fields.
 */
public class GraphQLImpl {

    private final DataFetchers fetchers;
    private final SchemaTypes types = new SchemaTypes();

    public GraphQLImpl(Api api, boolean stream) {
        this.fetchers = new DataFetchers(api, stream);
    }

    public GraphQLImpl(Api api) {
        this(api, false);
    }

    public GraphQLSchema getSchema() {
        return GraphQLSchema.newSchema()
                .query(queryType())
                .codeRegistry(codeRegistry())
                // NB: this needed because the date type is only
                // references via a type reference to avoid forward-
                // declaration problems...
                .additionalTypes(Sets.newHashSet(types.addressType, types.datePeriodType, types.systemEventType))
                .build();
    }

    private GraphQLCodeRegistry codeRegistry() {
        GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry()
                .typeResolver(types.entityInterface, types.entityTypeResolver)
                .typeResolver(types.describedInterface, types.entityTypeResolver)
                .typeResolver(types.descriptionInterface, types.descriptionTypeResolver)
                .typeResolver(types.annotatableInterface, types.entityTypeResolver)
                .typeResolver(types.linkableInterface, types.entityTypeResolver)
                .typeResolver(types.temporalDescriptionInterface, types.descriptionTypeResolver)
                .typeResolver(types.temporalInterface, types.entityTypeResolver);

        List<GraphQLObjectType> nodeTypes = Lists.newArrayList(
                types.documentaryUnitType,
                types.documentaryUnitDescriptionType,
                types.repositoryType,
                types.repositoryDescriptionType,
                types.historicalAgentType,
                types.historicalAgentDescriptionType,
                types.conceptType,
                types.conceptDescriptionType,
                types.countryType,
                types.vocabularyType,
                types.authoritativeSetType,
                types.linkType,
                types.annotationType,
                types.addressType,
                types.accessPointType,
                types.datePeriodType
        );

        for (GraphQLObjectType type : nodeTypes) {
            for (GraphQLFieldDefinition field : type.getFieldDefinitions()) {
                // Handle generic traversals...
                switch (field.getName()) {
                    case Bundle.ID_KEY:
                        builder.dataFetcher(type, field, idDataFetcher);
                        break;
                    case Bundle.TYPE_KEY:
                        builder.dataFetcher(type, field, typeDataFetcher);
                        break;
                    case "pid":
                        builder.dataFetcher(type, field, pidDataFetcher);
                        break;
                    case "ark":
                        builder.dataFetcher(type, field, arkDataFetcher);
                        break;
                    case "systemEvents":
                        builder.dataFetcher(type, field, fetchers.itemEventsDataFetcher());
                        break;
                    case "dates":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Temporal.class).getDatePeriods()));
                        break;
                    case "accessPoints":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Description.class).getAccessPoints()));
                        break;
                    case "addresses":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Addressable.class).getAddresses()));
                        break;
                    case "links":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Linkable.class).getLinks()));
                        break;
                    case "annotations":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Annotatable.class).getAnnotations()));
                        break;
                    case "descriptions":
                        builder.dataFetcher(coordinates(type, field),
                                fetchers.oneToManyRelationshipFetcher(r -> r.as(Described.class).getDescriptions()));
                        break;
                    case "description":
                        builder.dataFetcher(coordinates(type, field), descriptionDataFetcher);
                        break;
                    case "related": // deprecated
                    case "connected":
                        // NB: for CvocConcept types this is overridden below to return a
                        // set of CvocConcepts rather than Relationship objects
                        builder.dataFetcher(coordinates(type, field), connectedItemsDataFetcher);
                        break;
                    default:
                }
                // Add a default data fetcher which returns an object attribute...
                if (field.getType() instanceof graphql.schema.GraphQLList) {
                    builder.dataFetcherIfAbsent(coordinates(type, field), listDataFetcher(attributeDataFetcher));
                } else {
                    builder.dataFetcherIfAbsent(coordinates(type, field), attributeDataFetcher);
                }
            }
        }

        // Documentary Unit traversals...
        builder.dataFetchers(types.documentaryUnitType.getName(), ImmutableMap.of(
                "itemCount", fetchers.itemCountDataFetcher(c -> c.as(DocumentaryUnit.class).countChildren()),
                "repository", fetchers.manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getRepository()),
                "children", fetchers.hierarchicalOneToManyRelationshipConnectionFetcher(
                        d -> d.as(DocumentaryUnit.class).getChildren(), d -> d.as(DocumentaryUnit.class).getAllChildren()
                ),
                "parent", fetchers.manyToOneRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getParent()),
                "ancestors", fetchers.oneToManyRelationshipFetcher(d -> d.as(DocumentaryUnit.class).getAncestors())
        ));

        // Repository traversals
        builder.dataFetchers(types.repositoryType.getName(), ImmutableMap.of(
                "itemCount", fetchers.itemCountDataFetcher(c -> c.as(Repository.class).countChildren()),
                "documentaryUnits", fetchers.hierarchicalOneToManyRelationshipConnectionFetcher(
                        r -> r.as(Repository.class).getTopLevelDocumentaryUnits(),
                        r -> r.as(Repository.class).getAllDocumentaryUnits()),
                "country", fetchers.manyToOneRelationshipFetcher(r -> r.as(Repository.class).getCountry())
        ));

        // Country traversals
        builder.dataFetchers(types.countryType.getName(), ImmutableMap.<String, graphql.schema.DataFetcher<?>>builder()
                .put("itemCount", fetchers.itemCountDataFetcher(c -> c.as(Country.class).countChildren()))
                .put("name", transformingDataFetcher(idDataFetcher, LanguageHelpers::countryCodeToName))
                .put("repositories", fetchers.oneToManyRelationshipConnectionFetcher(c -> c.as(Country.class).getRepositories()))
                // Properties which do not match attribute names
                .put(CountryInfo.history.name(), keyDataFetcher("report"))
                .put(CountryInfo.summary.name(), keyDataFetcher("dataSummary"))
                .put(CountryInfo.extensive.name(), keyDataFetcher("dataExtensive"))
                .build());

        // Concept traversals
        // NB: due to an unfortunate mistake, the concept "related" field, which returns concepts
        // that are conceptually related, collides in naming with the generic "related" field, which
        // returns a set of "Relationship" items. The generic "related" field has now been deprecated
        // and renamed "connected"
        builder.dataFetchers(types.conceptType.getName(), ImmutableMap.of(
                "itemCount", fetchers.itemCountDataFetcher(c -> c.as(Concept.class).countChildren()),
                "vocabulary", fetchers.manyToOneRelationshipFetcher(c -> c.as(Concept.class).getVocabulary()),
                "related", fetchers.oneToManyRelationshipFetcher(c -> c.as(Concept.class).getRelatedConcepts()),
                "broader", fetchers.oneToManyRelationshipFetcher(c -> c.as(Concept.class).getBroaderConcepts()),
                "narrower", fetchers.oneToManyRelationshipFetcher(c -> c.as(Concept.class).getNarrowerConcepts())
        ));

        // Vocabularies traversals
        builder.dataFetchers(types.vocabularyType.getName(), ImmutableMap.of(
                "itemCount", fetchers.itemCountDataFetcher(c -> c.as(Vocabulary.class).countChildren()),
                "concepts", fetchers.oneToManyRelationshipConnectionFetcher(c -> c.as(Vocabulary.class).getConcepts())
        ));

        // AuthoritativeSet traversals
        builder.dataFetchers(types.authoritativeSetType.getName(), ImmutableMap.of(
                "itemCount", fetchers.itemCountDataFetcher(c -> c.as(AuthoritativeSet.class).countChildren()),
                "authorities", fetchers.oneToManyRelationshipConnectionFetcher(c -> c.as(AuthoritativeSet.class).getAuthoritativeItems())
        ));

        // Links
        builder.dataFetcher(coordinates(types.linkType.getName(), "targets"),
                fetchers.oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkTargets()));

        builder.dataFetcher(coordinates(types.linkType.getName(), "source"),
                fetchers.manyToOneRelationshipFetcher(a -> a.as(Link.class).getLinkSource()));

        builder.dataFetcher(coordinates(types.linkType.getName(), "linkType"),
                keyDataFetcher(Ontology.LINK_HAS_TYPE));

        builder.dataFetcher(coordinates(types.linkType.getName(), "body"),
                fetchers.oneToManyRelationshipFetcher(a -> a.as(Link.class).getLinkBodies()));

        // Annotations
        builder.dataFetcher(coordinates(types.annotationType.getName(), "targets"),
                fetchers.oneToManyRelationshipFetcher(a -> a.as(Annotation.class).getTargets()));

        // Hack: override type for access points, since it's not a node type
        builder.dataFetcher(coordinates(types.accessPointType.getName(), Ontology.ACCESS_POINT_TYPE), attributeDataFetcher);

        // Description field for link is a scalar attribute...
        builder.dataFetcher(coordinates(types.linkType.getName(), Ontology.LINK_HAS_DESCRIPTION), attributeDataFetcher);

        // Add a special field for annotation author
        builder.dataFetcher(coordinates(types.annotationType.getName(), "by"), annotationNameDataFetcher);

        // Top level data fetchers
        builder.dataFetchers("Root", ImmutableMap.<String, graphql.schema.DataFetcher<?>>builder()

                // Single items by ID
                .put(Entities.DOCUMENTARY_UNIT, fetchers.entityIdDataFetcher(Entities.DOCUMENTARY_UNIT))
                .put(Entities.REPOSITORY, fetchers.entityIdDataFetcher(Entities.REPOSITORY))
                .put(Entities.COUNTRY, fetchers.entityIdDataFetcher(Entities.COUNTRY))
                .put(Entities.HISTORICAL_AGENT, fetchers.entityIdDataFetcher(Entities.HISTORICAL_AGENT))
                .put(Entities.AUTHORITATIVE_SET, fetchers.entityIdDataFetcher(Entities.AUTHORITATIVE_SET))
                .put(Entities.CVOC_CONCEPT, fetchers.entityIdDataFetcher(Entities.CVOC_CONCEPT))
                .put(Entities.CVOC_VOCABULARY, fetchers.entityIdDataFetcher(Entities.CVOC_VOCABULARY))
                .put(Entities.ANNOTATION, fetchers.entityIdDataFetcher(Entities.ANNOTATION))
                .put(Entities.LINK, fetchers.entityIdDataFetcher(Entities.LINK))

                // PIDs
                .put("itemByPid", fetchers.entityPidDataFetcher())

                // Multiples
                .put("documentaryUnits", fetchers.docDataFetcher())
                .put("topLevelDocumentaryUnits", fetchers.topLevelDocDataFetcher())
                .put("repositories", fetchers.entityTypeConnectionDataFetcher(EntityClass.REPOSITORY))
                .put("historicalAgents", fetchers.entityTypeConnectionDataFetcher(EntityClass.HISTORICAL_AGENT))
                .put("countries", fetchers.entityTypeConnectionDataFetcher(EntityClass.COUNTRY))
                .put("authoritativeSets", fetchers.entityTypeConnectionDataFetcher(EntityClass.AUTHORITATIVE_SET))
                .put("concepts", fetchers.entityTypeConnectionDataFetcher(EntityClass.CVOC_CONCEPT))
                .put("vocabularies", fetchers.entityTypeConnectionDataFetcher(EntityClass.CVOC_VOCABULARY))
                .put("annotations", fetchers.entityTypeConnectionDataFetcher(EntityClass.ANNOTATION))
                .put("links", fetchers.entityTypeConnectionDataFetcher(EntityClass.LINK))
                .build());


        return builder.build();
    }

    private GraphQLObjectType queryType() {
        return newObject()
                .name("Root")

                // Single item types...
                .field(itemFieldDefinition(Entities.DOCUMENTARY_UNIT, __("root.single.documentaryUnit.description"),
                        types.documentaryUnitType, idArgument))
                .field(itemFieldDefinition(Entities.REPOSITORY, __("root.single.repository.description"),
                        types.repositoryType, idArgument))
                .field(itemFieldDefinition(Entities.COUNTRY, __("root.single.country.description"),
                        types.countryType, idArgument))
                .field(itemFieldDefinition(Entities.HISTORICAL_AGENT, __("root.single.historicalAgent.description"),
                        types.historicalAgentType, idArgument))
                .field(itemFieldDefinition(Entities.AUTHORITATIVE_SET, __("root.single.authoritativeSet.description"),
                        types.authoritativeSetType, idArgument))
                .field(itemFieldDefinition(Entities.CVOC_CONCEPT, __("root.single.cvocConcept.description"),
                        types.conceptType, idArgument))
                .field(itemFieldDefinition(Entities.CVOC_VOCABULARY, __("root.single.cvocVocabulary.description"),
                        types.vocabularyType, idArgument))
                .field(itemFieldDefinition(Entities.ANNOTATION, __("root.single.annotation.description"),
                        types.annotationType, idArgument))
                .field(itemFieldDefinition(Entities.LINK, __("root.single.link.description"),
                        types.linkType, idArgument))

                // PID lookups
                .field(itemFieldDefinition("itemByPid", "Lookup by PID", types.entityInterface, pidArgument))

                // Top level item connections
                .field(connectionFieldDefinition("documentaryUnits", __("root.connection.documentaryUnit.description"),
                        types.documentaryUnitsConnection, topLevelArgument))
                .field(connectionFieldDefinition("topLevelDocumentaryUnits", __("root.connection.documentaryUnit.topLevel.description"),
                        types.documentaryUnitsConnection)
                        .deprecate(__("root.connection.documentaryUnit.topLevel.deprecationReason")))
                .field(connectionFieldDefinition("repositories", __("root.connection.repository.description"),
                        types.repositoriesConnection))
                .field(connectionFieldDefinition("historicalAgents", __("root.connection.historicalAgent.description"),
                        types.historicalAgentsConnection))
                .field(connectionFieldDefinition("countries", __("root.connection.country.description"),
                        types.countriesConnection))
                .field(connectionFieldDefinition("authoritativeSets", __("root.connection.authoritativeSet.description"),
                        types.authoritativeSetsConnection))
                .field(connectionFieldDefinition("concepts", __("root.connection.cvocConcept.description"),
                        types.conceptsConnection))
                .field(connectionFieldDefinition("vocabularies", __("root.connection.cvocVocabulary.description"),
                        types.vocabulariesConnection))
                .field(connectionFieldDefinition("annotations", __("root.connection.annotation.description"),
                        types.annotationsConnection))
                .field(connectionFieldDefinition("links", __("root.connection.link.description"),
                        types.linksConnection))
                .build();
    }
}
