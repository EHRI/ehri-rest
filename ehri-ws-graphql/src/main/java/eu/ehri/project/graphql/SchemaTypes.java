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

import eu.ehri.project.definitions.*;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.LinkType;
import eu.ehri.project.models.base.*;
import graphql.TypeResolutionEnvironment;
import graphql.schema.*;

import java.util.List;

import static eu.ehri.project.graphql.FieldDefinitions.*;
import static eu.ehri.project.graphql.Messages.__;
import static graphql.Scalars.*;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * The shape of the GraphQL schema: every interface, object, enum type,
 * and type resolver, plus the connection wrapper types used for paginated
 * lists. This is pure schema structure - it has no dependency on the
 * {@link eu.ehri.project.api.Api}, unlike the {@link DataFetchers} that
 * are wired onto it in {@link GraphQLImpl#codeRegistry()}.
 */
final class SchemaTypes {

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

    final TypeResolver entityTypeResolver = new TypeResolver() {
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

    final TypeResolver descriptionTypeResolver = new TypeResolver() {
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

    final GraphQLInterfaceType entityInterface = newInterface()
            .name(Entity.class.getSimpleName())
            .description(__("graphql.interface.entity.description"))
            .fields(entityFields)
            .build();

    final GraphQLInterfaceType descriptionInterface = newInterface()
            .name(Description.class.getSimpleName())
            .description(__("graphql.interface.description.description"))
            .fields(descriptionFields())
            .build();

    final GraphQLInterfaceType temporalDescriptionInterface = newInterface()
            .name(Temporal.class.getSimpleName() + Description.class.getSimpleName())
            .description(__("graphql.interface.temporalDescription.description"))
            .fields(descriptionFields())
            .field((f) -> datePeriodFieldDefinition)
            .build();

    final GraphQLInterfaceType describedInterface = newInterface()
            .name(Described.class.getSimpleName())
            .description(__("graphql.interface.described.description"))
            .fields(entityFields)
            .field(singleDescriptionFieldDefinition(descriptionInterface))
            .field(descriptionsFieldDefinition(descriptionInterface))
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.identifier.description")))
            .fields(linksAndAnnotationsFields())
            .build();

    final GraphQLInterfaceType temporalInterface = newInterface()
            .name(Temporal.class.getSimpleName())
            .description(__("graphql.interface.temporal.description"))
            .fields(entityFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.identifier.description")))
            .field(singleDescriptionFieldDefinition(temporalDescriptionInterface))
            .field(descriptionsFieldDefinition(temporalDescriptionInterface))
            .build();

    final GraphQLInterfaceType annotatableInterface = newInterface()
            .fields(entityFields)
            .field(annotationsFieldDefinition)
            .name(Annotatable.class.getSimpleName())
            .description(__("graphql.interface.annotatable.description"))
            .build();

    final GraphQLInterfaceType linkableInterface = newInterface()
            .fields(entityFields)
            .field(linkFieldDefinition)
            .name(Linkable.class.getSimpleName())
            .description(__("graphql.interface.linkable.description"))
            .build();

    final GraphQLObjectType systemEventType = newObject()
            .name(Entities.SYSTEM_EVENT)
            .description(__("systemEvent.description"))
            .field(nonNullAttr(Ontology.EVENT_TIMESTAMP, Messages.bundle.getString("systemEvent.field.timestamp.description")))
            .field(nullAttr(Ontology.EVENT_LOG_MESSAGE, Messages.bundle.getString("systemEvent.field.logMessage.description")))
            .field(nonNullAttr(Ontology.EVENT_TYPE, Messages.bundle.getString("systemEvent.field.eventType.description")))
            .build();

    final GraphQLEnumType linkTypeEnum = newEnum()
            .name(LinkType.class.getSimpleName())
            .description(__("graphql.enum.linkType.description"))
            .value(LinkType.associative.name())
            .value(LinkType.hierarchical.name())
            .value(LinkType.temporal.name())
            .value(LinkType.identity.name())
            .value(LinkType.family.name())
            .value(LinkType.copy.name())
            .build();

    final GraphQLEnumType accessPointTypeEnum = newEnum()
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

    final GraphQLObjectType accessPointType = newObject()
            .name(Entities.ACCESS_POINT)
            .description(__("accessPoint.description"))
            .field(idField)
            .field(nonNullAttr(Ontology.NAME_KEY, __("accessPoint.field.name.description")))
            .field(newFieldDefinition()
                    .name(Ontology.ACCESS_POINT_TYPE)
                    .description(__("accessPoint.field.type.description"))
                    .type(GraphQLNonNull.nonNull(accessPointTypeEnum))
                    .build())
            .build();

    final GraphQLObjectType datePeriodType = newObject()
            .name(Entities.DATE_PERIOD)
            .description(__("datePeriod.description"))
            .field(nullAttr(Ontology.DATE_PERIOD_START_DATE, __("datePeriod.field.startDate.description")))
            .field(nullAttr(Ontology.DATE_PERIOD_END_DATE, __("datePeriod.field.endDate.description")))
            .build();

    final GraphQLObjectType addressType = newObject()
            .name(Entities.ADDRESS)
            .description(__("address.description"))
            .fields(nullStringAttrs(ContactInfo.values()))
            .fields(listStringAttrs(ContactInfo.values()))
            .build();

    final GraphQLObjectType connectedType = newObject()
            .name("Relationship")
            .description(__("relationship.description"))
            .field(newFieldDefinition()
                    .name("context")
                    .description(__("relationship.field.context.description"))
                    .type(GraphQLTypeReference.typeRef(Entities.LINK))
                    .build())
            .field(connectedItemsItemFieldDefinition())
            .build();

    final GraphQLObjectType documentaryUnitDescriptionType = newObject()
            .name(Entities.DOCUMENTARY_UNIT_DESCRIPTION)
            .description(__("documentaryUnitDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(datePeriodFieldDefinition)
            .fields(documentaryUnitDescriptionNullFields)
            .fields(documentaryUnitDescriptionListFields)
            .withInterfaces(descriptionInterface, temporalDescriptionInterface)
            .build();

    final GraphQLObjectType repositoryDescriptionType = newObject()
            .name(Entities.REPOSITORY_DESCRIPTION)
            .description(__("repositoryDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(listFieldDefinition("addresses", __("repositoryDescription.field.addresses.description"), addressType))
            .fields(repositoryDescriptionNullFields)
            .fields(repositoryDescriptionListFields)
            .withInterfaces(descriptionInterface)
            .build();

    final GraphQLObjectType historicalAgentDescriptionType = newObject()
            .name(Entities.HISTORICAL_AGENT_DESCRIPTION)
            .description(__("historicalAgentDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .field(datePeriodFieldDefinition)
            .fields(historicalAgentDescriptionNullFields)
            .fields(historicalAgentDescriptionListFields)
            .withInterfaces(descriptionInterface, temporalDescriptionInterface)
            .build();

    final GraphQLObjectType conceptDescriptionType = newObject()
            .name(Entities.CVOC_CONCEPT_DESCRIPTION)
            .description(__("conceptDescription.description"))
            .fields(descriptionFields())
            .field(accessPointFieldDefinition)
            .fields(conceptDescriptionNullFields)
            .fields(conceptDescriptionListFields)
            .withInterfaces(descriptionInterface)
            .build();

    final GraphQLObjectType repositoryType = newObject()
            .name(Entities.REPOSITORY)
            .description(__("repository.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("repository.field.identifier.description")))
            .field(itemCountFieldDefinition())
            .field(connectionFieldDefinition("documentaryUnits", __("repository.field.documentaryUnits.description"),
                    GraphQLTypeReference.typeRef("documentaryUnits"),
                    allArgument))
            .fields(geoFields)
            .field(singleDescriptionFieldDefinition(repositoryDescriptionType))
            .field(descriptionsFieldDefinition(repositoryDescriptionType))
            .field(itemFieldDefinition("country", __("repository.field.country.description"),
                    GraphQLTypeReference.typeRef(Entities.COUNTRY)))
            .fields(linksAndAnnotationsFields())
            .field(connectedTypeFieldDefinition())
            .field(deprecatedRelatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface)
            .build();

    final GraphQLObjectType documentaryUnitType = newObject()
            .name(Entities.DOCUMENTARY_UNIT)
            .description(__("documentaryUnit.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("documentaryUnit.field.identifier.description")))
            .field(listFieldDefinition(Ontology.OTHER_IDENTIFIERS, __("documentaryUnit.field.otherIdentifiers.description"), GraphQLString))
            .field(descriptionsFieldDefinition(documentaryUnitDescriptionType))
            .field(singleDescriptionFieldDefinition(documentaryUnitDescriptionType))
            .field(itemFieldDefinition("repository", __("documentaryUnit.field.repository.description"), repositoryType))
            .field(itemCountFieldDefinition())
            .field(connectionFieldDefinition("children", __("documentaryUnit.field.children.description"),
                    GraphQLTypeReference.typeRef("documentaryUnits"), allArgument))
            .field(itemFieldDefinition("parent", __("documentaryUnit.field.parent.description"),
                    GraphQLTypeReference.typeRef(Entities.DOCUMENTARY_UNIT)))
            .field(listFieldDefinition("ancestors", __("documentaryUnit.field.ancestors.description"),
                    GraphQLTypeReference.typeRef(Entities.DOCUMENTARY_UNIT)))
            .fields(linksAndAnnotationsFields())
            .field(connectedTypeFieldDefinition())
            .field(deprecatedRelatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface, temporalInterface)
            .build();

    final GraphQLObjectType historicalAgentType = newObject()
            .name(Entities.HISTORICAL_AGENT)
            .description(__("historicalAgent.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("historicalAgent.field.identifier.description")))
            .field(singleDescriptionFieldDefinition(historicalAgentDescriptionType))
            .field(descriptionsFieldDefinition(historicalAgentDescriptionType))
            .fields(linksAndAnnotationsFields())
            .field(connectedTypeFieldDefinition())
            .field(deprecatedRelatedTypeFieldDefinition())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface, temporalInterface)
            .build();

    final GraphQLObjectType authoritativeSetType = newObject()
            .name(Entities.AUTHORITATIVE_SET)
            .description(__("authoritativeSet.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("authoritativeSet.field.identifier.description")))
            .field(nonNullAttr(Ontology.NAME_KEY, __("authoritativeSet.field.name.description")))
            .field(nullAttr("description", __("authoritativeSet.field.description.description")))
            .field(itemCountFieldDefinition())
            .field(connectionFieldDefinition("authorities", __("authoritativeSet.field.authorities.description"),
                    GraphQLTypeReference.typeRef("historicalAgents")))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    final GraphQLObjectType countryType = newObject()
            .name(Entities.COUNTRY)
            .description(__("country.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("country.field.identifier.description")))
            .field(newFieldDefinition()
                    .name(Ontology.NAME_KEY)
                    .description(__("country.field.name.description"))
                    .type(GraphQLNonNull.nonNull(GraphQLString))
                    .build()
            )
            .fields(countryDescriptionNullFields)
            .fields(countryDescriptionListFields)
            .field(itemCountFieldDefinition())
            .field(connectionFieldDefinition("repositories", __("country.field.repositories.description"),
                    GraphQLTypeReference.typeRef("repositories")))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface, linkableInterface)
            .build();

    final GraphQLObjectType conceptType = newObject()
            .name(Entities.CVOC_CONCEPT)
            .description(__("cvocConcept.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("cvocConcept.field.identifier.description")))
            .fields(conceptNullFields)
            .fields(conceptListFields)
            .fields(geoFields)
            .field(descriptionsFieldDefinition(conceptDescriptionType))
            .field(singleDescriptionFieldDefinition(conceptDescriptionType))
            .field(itemCountFieldDefinition())
            .field(listFieldDefinition("broader", __("cvocConcept.field.broader.description"),
                    GraphQLTypeReference.typeRef(Entities.CVOC_CONCEPT)))
            .field(listFieldDefinition("related", __("cvocConcept.field.related.description"),
                    GraphQLTypeReference.typeRef(Entities.CVOC_CONCEPT)))
            .field(listFieldDefinition("narrower", __("cvocConcept.field.narrower.description"),
                    GraphQLTypeReference.typeRef(Entities.CVOC_CONCEPT)))
            .field(itemFieldDefinition("vocabulary", __("cvocConcept.field.vocabulary.description"),
                    GraphQLTypeReference.typeRef(Entities.CVOC_VOCABULARY)))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .field(connectedTypeFieldDefinition())
            .withInterfaces(entityInterface, describedInterface, linkableInterface, annotatableInterface)
            .build();

    final GraphQLObjectType vocabularyType = newObject()
            .name(Entities.CVOC_VOCABULARY)
            .description(__("cvocVocabulary.description"))
            .fields(entityFields)
            .fields(pidFields)
            .field(nonNullAttr(Ontology.IDENTIFIER_KEY, __("cvocVocabulary.field.identifier.description")))
            .field(nonNullAttr(Ontology.NAME_KEY, __("cvocVocabulary.field.name.description")))
            .field(nullAttr("description", __("cvocVocabulary.field.description.description")))
            .field(itemCountFieldDefinition())
            .field(connectionFieldDefinition("concepts", __("cvocVocabulary.field.concepts.description"),
                    GraphQLTypeReference.typeRef("concepts")))
            .fields(linksAndAnnotationsFields())
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    final GraphQLObjectType annotationType = newObject()
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
                    .build()
            )
            .field(listFieldDefinition("targets", __("annotation.field.targets.description"),
                    annotatableInterface))
            .field(listFieldDefinition("annotations", __("annotation.field.annotations.description"),
                    GraphQLTypeReference.typeRef(Entities.ANNOTATION)))
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    final GraphQLObjectType linkType = newObject()
            .name(Entities.LINK)
            .description(__("link.description"))
            .fields(entityFields)
            .field(nullAttr(Ontology.LINK_HAS_DESCRIPTION, __("link.field.description.description")))
            .field(nullAttr(Ontology.LINK_HAS_FIELD, __("link.field.field.description")))
            .field(listFieldDefinition("targets", __("link.field.targets.description"), linkableInterface))
            .field(itemFieldDefinition("source", __("link.field.source.description"), linkableInterface))
            .field(listFieldDefinition("body", __("link.field.body.description"), accessPointType))
            .field(newFieldDefinition()
                    .name("linkType")
                    .description(__("graphql.enum.linkType.description"))
                    .type(GraphQLNonNull.nonNull(linkTypeEnum))
                    .build())
            .field(annotationsFieldDefinition)
            .field(datePeriodFieldDefinition)
            .field(itemEventsFieldDefinition())
            .withInterfaces(entityInterface, annotatableInterface)
            .build();

    final GraphQLOutputType documentaryUnitsConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.DOCUMENTARY_UNIT), "documentaryUnits");

    final GraphQLOutputType repositoriesConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.REPOSITORY), "repositories");

    final GraphQLOutputType countriesConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.COUNTRY), "countries");

    final GraphQLOutputType historicalAgentsConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.HISTORICAL_AGENT), "historicalAgents");

    final GraphQLOutputType authoritativeSetsConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.AUTHORITATIVE_SET), "authoritativeSets");

    final GraphQLOutputType conceptsConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.CVOC_CONCEPT), "concepts");

    final GraphQLOutputType vocabulariesConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.CVOC_VOCABULARY), "vocabularies");

    final GraphQLOutputType annotationsConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.ANNOTATION), "annotations");

    final GraphQLOutputType linksConnection = connectionType(
            GraphQLTypeReference.typeRef(Entities.LINK), "links");

    // Field definitions that reference this schema's own types, so unlike
    // the rest of FieldDefinitions they can't be schema-agnostic statics.

    private GraphQLFieldDefinition connectedTypeFieldDefinition() {
        return newFieldDefinition()
                .argument(GraphQLArgument.newArgument()
                        .name("linkType")
                        .description(__("link.field.linkType.description"))
                        .type(linkTypeEnum)
                        .build()
                )
                .name("connected")
                .description(__("graphql.field.connected.description"))
                .type(GraphQLList.list(connectedType))
                .build();
    }

    private GraphQLFieldDefinition deprecatedRelatedTypeFieldDefinition() {
        return newFieldDefinition()
                .name("related")
                .description(__("graphql.field.related.description"))
                .type(GraphQLList.list(connectedType))
                .deprecate(__("graphql.field.related.deprecated"))
                .build();
    }

    private GraphQLFieldDefinition connectedItemsItemFieldDefinition() {
        return newFieldDefinition()
                .type(linkableInterface)
                .name("item")
                .description(__("graphql.field.connected.item.description"))
                .build();
    }
}
