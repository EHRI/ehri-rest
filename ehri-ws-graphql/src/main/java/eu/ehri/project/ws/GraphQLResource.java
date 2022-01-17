package eu.ehri.project.ws;

import com.fasterxml.jackson.core.JsonGenerator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.core.Tx;
import eu.ehri.project.graphql.GraphQLImpl;
import eu.ehri.project.graphql.GraphQLQuery;
import eu.ehri.project.graphql.StreamingExecutionStrategy;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.ws.base.AbstractAccessibleResource;
import eu.ehri.project.ws.errors.ExecutionError;
import graphql.*;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * GraphQL implementation.
 */
@Path(GraphQLResource.ENDPOINT)
public class GraphQLResource extends AbstractAccessibleResource<Accessible> {

    public static final String ENDPOINT = "graphql";

    private static final Config config = ConfigFactory.load();

    private static final int MAX_DEPTH = config.getInt("graphql.limits.maxDepth");
    private static final int MAX_DEPTH_ANON = config.getInt("graphql.limits.maxDepthAnonymous");
    private static final int MAX_COMPLEXITY = config.getInt("graphql.limits.maxComplexity");
    private static final int MAX_COMPLEXITY_ANON = config.getInt("graphql.limits.maxComplexityAnonymous");

    public GraphQLResource(@Context GraphDatabaseService database) {
        super(database, Accessible.class);
    }

    // Helpers

    /**
     * Fetch the introspected GraphQL schema.
     *
     * @return JSON data describing the schema.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ExecutionResult describe() {
        try (final Tx tx = beginTx()) {
            GraphQLSchema schema = new GraphQLImpl(api()).getSchema();
            tx.success();
            return GraphQL.newGraphQL(schema).build()
                    .execute(IntrospectionQuery.INTROSPECTION_QUERY);
        }
    }

    /**
     * Run a GraphQL query.
     *
     * @param q a query object containing <code>query</code> and <code>variables</code>
     *          fields
     * @return the results of the query as JSON
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(GraphQLQuery q) {
        try (final Tx tx = beginTx()) {
            boolean stream = isStreaming();
            GraphQLSchema schema = new GraphQLImpl(api(), stream).getSchema();
            Object data = stream ? lazyExecution(schema, q) : strictExecution(schema, q);
            tx.success();
            return Response.ok(data).build();
        }
    }

    /**
     * Run a GraphQL query.
     *
     * @param q a GraphQL query as text.
     * @return the results of the query as JSON
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(String q) {
        return query(new GraphQLQuery(q));
    }

    private ExecutionResult strictExecution(GraphQLSchema schema, GraphQLQuery q) {
        ExecutionResult executionResult = GraphQL
                .newGraphQL(schema)
                .instrumentation(getInstrumentation())
                .build()
                .execute(ExecutionInput.newExecutionInput()
                        .query(q.getQuery())
                        .operationName(q.getOperationName())
                        .variables(q.getVariables()).build());
        if (!executionResult.getErrors().isEmpty()) {
            throw new ExecutionError(executionResult.getErrors());
        }
        return executionResult;
    }

    private StreamingOutput lazyExecution(GraphQLSchema schema, GraphQLQuery q) {
        final ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(q.getQuery())
                .operationName(q.getOperationName())
                .variables(q.getVariables())
                .build();
        ParseAndValidateResult validator = ParseAndValidate.parseAndValidate(schema, input);
        if (validator.isFailure()) {
            throw new ExecutionError(validator.getErrors());
        }

        return outputStream -> {
            try (final Tx tx = beginTx();
                 final JsonGenerator generator = jsonFactory.createGenerator(outputStream).useDefaultPrettyPrinter()) {
                StreamingExecutionStrategy strategy = StreamingExecutionStrategy.jsonGenerator(generator);

                final GraphQL graphQL = GraphQL
                        .newGraphQL(schema)
                        .instrumentation(getInstrumentation())
                        .queryExecutionStrategy(strategy)
                        .build();
                graphQL.execute(input);
                tx.success();
            }
        };
    }

    private Instrumentation getInstrumentation() {
        final boolean anonymous = getRequesterUserProfile().isAnonymous();
        return new ChainedInstrumentation(
            new MaxQueryComplexityInstrumentation(anonymous ? MAX_COMPLEXITY_ANON : MAX_COMPLEXITY),
            new MaxQueryDepthInstrumentation(anonymous ? MAX_DEPTH_ANON : MAX_DEPTH));
    }
}
