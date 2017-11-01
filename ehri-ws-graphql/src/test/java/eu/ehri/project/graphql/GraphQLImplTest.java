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
        GraphQLImpl graphQL = new GraphQLImpl(manager, anonApi());
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery.graphql");
        ExecutionResult result = new GraphQL(schema)
                .execute(testQuery);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testDateQuery() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(manager, anonApi());
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery-dates.graphql");
        ExecutionResult result = GraphQL.newGraphQL(schema).build()
                .execute(testQuery);
        System.out.println(result.getData().toString());
    }
}