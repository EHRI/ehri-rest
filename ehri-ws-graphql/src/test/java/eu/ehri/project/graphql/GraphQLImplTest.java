package eu.ehri.project.graphql;

import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphQLImplTest extends AbstractFixtureTest {
    @Test
    public void testGetSchema() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(anonApi());
        GraphQLSchema schema = graphQL.getSchema();
        String testQuery = readResourceFileAsString("testquery.graphql");
        ExecutionResult result = new GraphQL(schema, new LazyExecutionStrategy())
                .execute(testQuery);
        assertTrue(result.getErrors().isEmpty());
    }
}