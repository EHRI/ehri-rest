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

import com.typesafe.config.ConfigFactory;
import graphql.ExecutionInput;
import graphql.GraphQLError;
import graphql.GraphqlErrorException;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Document;
import graphql.language.NodeUtil;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static eu.ehri.project.graphql.GraphQLConstants.IDS_PARAM;
import static eu.ehri.project.graphql.GraphQLConstants.TOP_LEVEL_PARAM;
import static eu.ehri.project.graphql.Messages.__;

/**
 * Checks on argument values which schema validation does not cover. These
 * must run before execution, since errors raised during streaming execution
 * cannot be reported.
 */
public final class QueryChecks {

    static final int MAX_IDS = ConfigFactory.load().getInt("graphql.limits.maxIds");

    private QueryChecks() {
    }

    /**
     * Check a parsed and validated query's argument values.
     *
     * @param schema   the schema
     * @param document the parsed query document
     * @param input    the execution input, providing the operation name and variables
     * @return a list of errors, empty if the query is acceptable
     */
    public static List<GraphQLError> check(GraphQLSchema schema, Document document, ExecutionInput input) {
        List<GraphQLError> errors = new ArrayList<>();
        try {
            // The schema has no mutation or subscription types, which the traverser requires
            OperationDefinition operation = NodeUtil.getOperation(document, input.getOperationName()).operationDefinition;
            if (operation.getOperation() != OperationDefinition.Operation.QUERY) {
                return Collections.singletonList(GraphqlErrorException.newErrorException()
                        .message(__("graphql.error.queryOnly"))
                        .sourceLocation(operation.getSourceLocation())
                        .build());
            }
            QueryTraverser.newQueryTraverser()
                    .schema(schema)
                    .document(document)
                    .operationName(input.getOperationName())
                    .variables(input.getVariables())
                    .build()
                    .visitPreOrder(new QueryVisitorStub() {
                        @Override
                        public void visitField(QueryVisitorFieldEnvironment env) {
                            Object ids = env.getArguments().get(IDS_PARAM);
                            if (ids != null && Boolean.TRUE.equals(env.getArguments().get(TOP_LEVEL_PARAM))) {
                                errors.add(error(env, String.format(__("graphql.error.idsWithTopLevel"),
                                        env.getField().getResultKey())));
                            }
                            if (ids instanceof Collection) {
                                int count = ((Collection<?>) ids).size();
                                if (count > MAX_IDS) {
                                    errors.add(error(env, String.format(__("graphql.error.tooManyIds"),
                                            env.getField().getResultKey(), count, MAX_IDS)));
                                }
                            }
                        }
                    });
        } catch (RuntimeException e) {
            // e.g. unknown operation names or variables which cannot be coerced
            if (e instanceof GraphQLError) {
                return Collections.singletonList((GraphQLError) e);
            }
            throw e;
        }
        return errors;
    }

    private static GraphQLError error(QueryVisitorFieldEnvironment env, String message) {
        return GraphqlErrorException.newErrorException()
                .message(message)
                .sourceLocation(env.getField().getSourceLocation())
                .build();
    }
}
