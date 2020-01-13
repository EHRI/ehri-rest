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

package eu.ehri.extension.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import graphql.GraphQLError;
import graphql.GraphqlErrorHelper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionError extends WebApplicationException {
    private static final ObjectMapper mapper = new ObjectMapper();

    public ExecutionError(List<? extends GraphQLError> errors) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(errorToJson(errors).getBytes(Charsets.UTF_8)).build());
    }

    private static String errorToJson(List<? extends GraphQLError> errors) {
        try {
            return mapper.writeValueAsString(
                    ImmutableMap.of("errors", errors
                            .stream()
                            .map(GraphqlErrorHelper::toSpecification)
                            .collect(Collectors.toList())
                    )
            );
        } catch (JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }
}
