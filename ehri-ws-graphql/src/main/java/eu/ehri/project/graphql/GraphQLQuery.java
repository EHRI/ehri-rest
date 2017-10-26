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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class GraphQLQuery {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String query;
    private final String variables;
    private final String operationName;

    private static final TypeReference<Map<String, Object>> ref = new TypeReference<Map<String, Object>>() {
    };

    @JsonCreator
    public GraphQLQuery(@JsonProperty("query") String query,
            @JsonProperty("variables") String variables,
            @JsonProperty("operationName") String operationName) {
        this.query = query;
        this.variables = variables;
        this.operationName = operationName;
    }

    public GraphQLQuery(String query) {
        this(query, "\"{}\"", null);
    }

    public String getQuery() {
        return query;
    }

    @JsonIgnore
    public Map<String, Object> getVariablesAsMap() {
        return deserializeVariables(variables);
    }

    public String getVariables() {
        return variables;
    }

    public String getOperationName() {
        return operationName;
    }

    @Override
    public String toString() {
        Map<String, Object> m = Maps.newHashMap();
        m.put("query", query);
        m.put("variables", variables);
        return m.toString();
    }

    private static Map<String, Object> deserializeVariables(String s) {
        try {
            Map<String, Object> value = mapper.readValue(s == null ? "" : s, ref);
            return value != null ? value : Collections.emptyMap();
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }
}
