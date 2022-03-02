/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

public class GraphQLQuery {

    private final String query;
    private final Map<String, Object> variables;
    private final String operationName;

    @JsonCreator
    public GraphQLQuery(@JsonProperty("query") String query,
            @JsonProperty("variables") Map<String, Object> variables,
            @JsonProperty("operationName") String operationName) {
        this.query = query;
        this.variables = variables;
        this.operationName = operationName;
    }

    public GraphQLQuery(String query) {
        this(query, Collections.emptyMap(), null);
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVariables() {
        return variables != null ? variables : Collections.emptyMap();
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
}
