package eu.ehri.project.ws;

import com.fasterxml.jackson.core.JsonGenerator;
import eu.ehri.project.ws.base.AbstractAccessibleResource;
import eu.ehri.project.ws.errors.ExecutionError;
import eu.ehri.project.core.Tx;
import eu.ehri.project.graphql.GraphQLImpl;
import eu.ehri.project.graphql.GraphQLQuery;
import eu.ehri.project.graphql.StreamingGraphQL;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.persistence.Bundle;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionQuery;
import graphql.language.Document;
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
        ExecutionResult executionResult = GraphQL.newGraphQL(schema).build()
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
        // FIXME: Ugly: have to reinitialise the schema in this transaction
        // otherwise iterables will be invalid.
        final StreamingGraphQL ql = new StreamingGraphQL(schema);
        // Check parsing, we have to do this again as well :(
        ql.parseAndValidate(q.getQuery(), q.getOperationName(), q.getVariables());
        return outputStream -> {
            try (final Tx tx = beginTx();
                 final JsonGenerator generator = jsonFactory.createGenerator(outputStream).useDefaultPrettyPrinter()) {
                final StreamingGraphQL ql2 = new StreamingGraphQL(new GraphQLImpl(api(), true).getSchema());
                Document document = ql2.parseAndValidate(q.getQuery(), q.getOperationName(), q.getVariables());
                generator.writeStartObject();
                generator.writeFieldName(Bundle.DATA_KEY);
                ql2.execute(generator, q.getQuery(), document, q.getOperationName(),
                        null, q.getVariables());
                generator.writeEndObject();
                tx.success();
            }
        };
    }
}
