package eu.ehri.project.graphql;

import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcherFactory;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class GraphQLImplTest extends AbstractFixtureTest {
    @Test
    public void testGetSchema() throws Exception {
        GraphQLImpl graphQL = new GraphQLImpl(anonApi());
        GraphQLSchema schema = graphQL.getSchema();

        GraphQLCodeRegistry codeRegistry = schema.getCodeRegistry();
        Field field = codeRegistry.getClass().getDeclaredField("dataFetcherMap");
        field.setAccessible(true);
        Map<FieldCoordinates, DataFetcherFactory<?>> dataFetchers = (Map)field.get(codeRegistry);
//        System.out.println(dataFetchers);
        for (Map.Entry<FieldCoordinates, DataFetcherFactory<?>> entry : dataFetchers.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        String testQuery = readResourceFileAsString("testquery.graphql");
        ExecutionResult result = GraphQL.newGraphQL(schema).build().execute(testQuery);
         System.out.println(result);
        assertTrue(result.getErrors().isEmpty());
    }
}