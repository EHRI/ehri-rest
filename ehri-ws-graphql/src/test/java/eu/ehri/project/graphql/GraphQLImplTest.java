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

import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GraphQLImplTest extends AbstractFixtureTest {
    @Test
    public void testGetSchema() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(api(adminUser));
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery.graphql");
        ExecutionResult result = GraphQL.newGraphQL(schema).build().execute(testQuery);
//         System.out.println(result);
        assertTrue(result.getErrors().isEmpty());
    }
}