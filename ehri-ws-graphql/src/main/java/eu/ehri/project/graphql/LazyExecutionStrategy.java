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

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLException;
import graphql.execution.ExecutionContext;
import graphql.execution.SimpleExecutionStrategy;
import graphql.language.Field;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stopgap class overriding method completeValueForList to handle
 * iterable values. Should be removed when https://git.io/vPBZy is
 * merged into a new graphql-java release.
 */
public class LazyExecutionStrategy extends SimpleExecutionStrategy {
    @Override
    protected ExecutionResult completeValue(ExecutionContext executionContext, GraphQLType fieldType, List<Field> fields, Object result) {
        if (fieldType instanceof GraphQLNonNull) {
            GraphQLNonNull graphQLNonNull = (GraphQLNonNull) fieldType;
            ExecutionResult completed = completeValue(executionContext, graphQLNonNull.getWrappedType(), fields, result);
            if (completed == null) {
                throw new GraphQLException("Cannot return null for non-nullable type: " + fields);
            }
            return completed;

        } else if (result == null) {
            return null;
        } else if (fieldType instanceof GraphQLList) {
            return completeValueForList(executionContext, (GraphQLList) fieldType, fields, result);
        } else if (fieldType instanceof GraphQLScalarType) {
            return completeValueForScalar((GraphQLScalarType) fieldType, result);
        } else if (fieldType instanceof GraphQLEnumType) {
            return completeValueForEnum((GraphQLEnumType) fieldType, result);
        }


        GraphQLObjectType resolvedType;
        if (fieldType instanceof GraphQLInterfaceType) {
            resolvedType = resolveType((GraphQLInterfaceType) fieldType, result);
        } else if (fieldType instanceof GraphQLUnionType) {
            resolvedType = resolveType((GraphQLUnionType) fieldType, result);
        } else {
            resolvedType = (GraphQLObjectType) fieldType;
        }

        Map<String, List<Field>> subFields = new LinkedHashMap<String, List<Field>>();
        List<String> visitedFragments = new ArrayList<String>();
        for (Field field : fields) {
            if (field.getSelectionSet() == null) continue;
            fieldCollector.collectFields(executionContext, resolvedType, field.getSelectionSet(), visitedFragments, subFields);
        }

        // Calling this from the executionContext so that you can shift from the simple execution strategy for mutations
        // back to the desired strategy.

        return executionContext.getExecutionStrategy().execute(executionContext, resolvedType, result, subFields);
    }

    private ExecutionResult completeValueForList(ExecutionContext executionContext, GraphQLList fieldType, List<Field> fields, Object result) {
        if (result.getClass().isArray()) {
            result = Arrays.asList((Object[]) result);
        }

        return completeValueForList(executionContext, fieldType, fields, (Iterable<Object>) result);
    }

    private ExecutionResult completeValueForList(ExecutionContext executionContext, GraphQLList fieldType, List<Field>
            fields, Iterable<Object> result) {
        List<Object> completedResults = new ArrayList<Object>();
        for (Object item : result) {
            ExecutionResult completedValue = completeValue(executionContext, fieldType.getWrappedType(), fields, item);
            completedResults.add(completedValue != null ? completedValue.getData() : null);
        }
        return new ExecutionResultImpl(completedResults, null);
    }
}
