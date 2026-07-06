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
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.DefinitionList;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Geo;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import graphql.schema.*;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import static eu.ehri.project.graphql.GraphQLConstants.*;
import static eu.ehri.project.graphql.Messages.__;
import static graphql.Scalars.*;
import static graphql.scalars.java.JavaPrimitives.GraphQLBigDecimal;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Reusable, schema-agnostic builders for GraphQL field and argument
 * definitions shared across the various entity types.
 */
final class FieldDefinitions {

    private FieldDefinitions() {
    }

    // Scalars and list/non-null wrappers...

    static final GraphQLList GraphQLStringList = GraphQLList.list(GraphQLString);
    static final GraphQLNonNull GraphQLNonNullString = GraphQLNonNull.nonNull(GraphQLString);

    static final GraphQLScalarType CursorType =
            GraphQLScalarType.newScalar()
                    .name("Cursor")
                    .description(__("cursor.description"))
                    .coercing(GraphQLString.getCoercing())
                    .build();

    // Common arguments...

    static final GraphQLArgument idArgument = newArgument()
            .name(Bundle.ID_KEY)
            .description(__("graphql.argument.id.description"))
            .type(GraphQLNonNull.nonNull(GraphQLID))
            .build();

    static final GraphQLArgument pidArgument = newArgument()
            .name("pid")
            .description(__("graphql.argument.pid.description"))
            .type(GraphQLNonNull.nonNull(GraphQLID))
            .build();

    static final GraphQLArgument allArgument = newArgument()
            .name(ALL_PARAM)
            .description(__("graphql.argument.all"))
            .type(GraphQLBoolean)
            .defaultValueProgrammatic(false)
            .build();

    static final GraphQLArgument topLevelArgument = newArgument()
            .name(TOP_LEVEL_PARAM)
            .description(__("graphql.argument.topLevel"))
            .type(GraphQLBoolean)
            .defaultValueProgrammatic(false)
            .build();

    // Field builder helpers...

    static GraphQLFieldDefinition.Builder nullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description);
    }

    static GraphQLFieldDefinition.Builder nullAttr(String name, String description) {
        return nullAttr(name, description, GraphQLString);
    }

    static GraphQLFieldDefinition.Builder nonNullAttr(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .type(type)
                .name(name)
                .description(description);
    }

    static GraphQLFieldDefinition.Builder nonNullAttr(String name, String description) {
        return nonNullAttr(name, description, GraphQLString);
    }

    static List<GraphQLFieldDefinition> nullStringAttrs(DefinitionList[] items) {
        return Lists.newArrayList(items)
                .stream().filter(i -> !i.isMultiValued())
                .map(f ->
                        newFieldDefinition()
                                .type(GraphQLString)
                                .name(f.name())
                                .description(f.getDescription())
                                .build()
                ).collect(Collectors.toList());
    }

    static List<GraphQLFieldDefinition> listStringAttrs(DefinitionList[] items) {
        return Lists.newArrayList(items)
                .stream().filter(DefinitionList::isMultiValued)
                .map(f ->
                        newFieldDefinition()
                                .type(GraphQLStringList)
                                .name(f.name())
                                .description(f.getDescription())
                                .build()
                ).collect(Collectors.toList());
    }

    static final GraphQLFieldDefinition idField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name(Bundle.ID_KEY)
            .description(__("graphql.field.id.description"))
            .build();

    static final GraphQLFieldDefinition typeField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name(Bundle.TYPE_KEY)
            .description(__("graphql.field.type.description"))
            .build();

    static final GraphQLFieldDefinition pidField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name("pid")
            .description(__("graphql.field.pid.description"))
            .build();

    static final GraphQLFieldDefinition arkField = newFieldDefinition()
            .type(GraphQLNonNullString)
            .name("ark")
            .description(__("graphql.field.ark.description"))
            .build();

    static GraphQLFieldDefinition.Builder singleDescriptionFieldDefinition(GraphQLOutputType descriptionType) {
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
                        .name(LANG_PARAM)
                        .description(__("graphql.argument.lang.description"))
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
                        .defaultValueProgrammatic(1)
                        .build()
                )
                .description(__("graphl.field.description.description"));
    }

    static GraphQLFieldDefinition.Builder listFieldDefinition(String name, String description, GraphQLOutputType type) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLList.list(type))
                .description(description)
                ;
    }

    static GraphQLFieldDefinition.Builder itemEventsFieldDefinition() {
        return newFieldDefinition()
                .name("systemEvents")
                .description(__("graphql.field.systemEvents.description"))
                .type(GraphQLList.list(GraphQLTypeReference.typeRef(Entities.SYSTEM_EVENT)))
                ;
    }

    static GraphQLFieldDefinition.Builder connectionFieldDefinition(String name, String description,
                                                                     GraphQLOutputType type, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .description(description)
                .type(type)
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
                .arguments(Lists.newArrayList(arguments));
    }

    static final List<GraphQLFieldDefinition> entityFields =
            ImmutableList.of(idField, typeField);

    static final List<GraphQLFieldDefinition> pidFields =
            ImmutableList.of(pidField, arkField);

    static final List<GraphQLFieldDefinition> geoFields = ImmutableList.of(
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

    static List<GraphQLFieldDefinition> descriptionFields() {
        return Lists.newArrayList(
                nonNullAttr(Ontology.LANGUAGE_OF_DESCRIPTION, __("graphql.field.languageCode.description")).build(),
                nonNullAttr(Ontology.NAME_KEY, __("graphql.field.name.description")).build(),
                nullAttr(Ontology.IDENTIFIER_KEY, __("graphql.field.description.identifier.description")).build()
        );
    }

    static GraphQLFieldDefinition.Builder descriptionsFieldDefinition(GraphQLOutputType descriptionType) {
        return newFieldDefinition()
                .type(GraphQLList.list(descriptionType))
                .name("descriptions")
                .description(__("graphql.field.descriptions.description"));
    }

    static GraphQLFieldDefinition.Builder itemCountFieldDefinition() {
        return newFieldDefinition()
                .type(GraphQLNonNull.nonNull(GraphQLInt))
                .name("itemCount")
                .description(__("graphql.field.itemCount.description"));
    }

    static final GraphQLFieldDefinition.Builder linkFieldDefinition =
            listFieldDefinition("links", __("graphql.field.links.description"),
                    GraphQLTypeReference.typeRef(Entities.LINK));

    static final GraphQLFieldDefinition.Builder annotationsFieldDefinition =
            listFieldDefinition("annotations", __("graphql.field.annotations.description"),
                    GraphQLTypeReference.typeRef(Entities.ANNOTATION));

    static List<GraphQLFieldDefinition> linksAndAnnotationsFields() {
        return Lists.newArrayList(linkFieldDefinition.build(), annotationsFieldDefinition.build());
    }

    static final GraphQLFieldDefinition.Builder accessPointFieldDefinition =
            listFieldDefinition("accessPoints", __("graphql.field.accessPoints.description"),
                    GraphQLTypeReference.typeRef(Entities.ACCESS_POINT));

    static final GraphQLFieldDefinition.Builder datePeriodFieldDefinition =
            listFieldDefinition("dates", __("graphql.field.dates.description"),
                    GraphQLTypeReference.typeRef(Entities.DATE_PERIOD));

    static GraphQLFieldDefinition.Builder itemFieldDefinition(String name, String description,
                                                               GraphQLOutputType type, GraphQLArgument... arguments) {
        return newFieldDefinition()
                .name(name)
                .type(type)
                .description(description)
                .arguments(Lists.newArrayList(arguments));
    }

    // Connections...

    static GraphQLOutputType edgeType(GraphQLTypeReference wrapped) {
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

    static List<GraphQLFieldDefinition> connectionFields(GraphQLTypeReference wrappedType) {
        return ImmutableList.of(
                newFieldDefinition()
                        .name(ITEMS)
                        .description(MessageFormat.format(__("graphql.connection.field.items.description"), wrappedType.getName()))
                        .type(GraphQLList.list(wrappedType))
                        .build(),
                newFieldDefinition()
                        .name(EDGES)
                        .description(MessageFormat.format(__("graphql.connection.field.edges.description"), wrappedType.getName()))
                        .type(GraphQLList.list(edgeType(wrappedType)))
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

    static GraphQLOutputType connectionType(GraphQLTypeReference wrappedType, String name) {
        return newObject()
                .name(name)
                .description(MessageFormat.format(__("graphql.connection.description"), wrappedType.getName()))
                .fields(connectionFields(wrappedType))
                .build();
    }
}
