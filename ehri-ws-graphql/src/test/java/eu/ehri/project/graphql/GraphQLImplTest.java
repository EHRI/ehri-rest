package eu.ehri.project.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphQLImplTest extends AbstractFixtureTest {
    @Test
    public void testGetSchema() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(manager, anonApi());
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery.graphql");
        ExecutionResult result = GraphQL.newGraphQL(schema).build().execute(testQuery);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testDateQuery() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(manager, api(validUser));
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery-dates.graphql");
        ExecutionResult result = GraphQL.newGraphQL(schema).build().execute(testQuery);
        //System.out.println(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        JsonNode node = new ObjectMapper().convertValue(data, JsonNode.class);
        //System.out.println(node);
        assertEquals(1, node.path("from_to_1").path("items").size());
        assertEquals("c1", node.path("from_to_1").path("items").path(0).path("id").asText());
        assertEquals(2, node.path("from_to_2").path("items").size());
        assertEquals(3, node.path("from").path("items").size());
        assertEquals(4, node.path("to").path("items").size());
        assertEquals(4, node.path("all").path("items").size());
    }
}