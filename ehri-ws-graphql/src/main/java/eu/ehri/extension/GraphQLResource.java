package eu.ehri.extension;

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.errors.ExecutionError;
import eu.ehri.extension.errors.ExecutionException;
import eu.ehri.project.core.Tx;
import eu.ehri.project.graphql.GraphQLImpl;
import eu.ehri.project.graphql.GraphQLQuery;
import eu.ehri.project.models.base.Accessible;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLException;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path(GraphQLResource.ENDPOINT)
public class GraphQLResource extends AbstractAccessibleResource<Accessible> {

    public static final String ENDPOINT = "graphql";

    public GraphQLResource(@Context GraphDatabaseService database) {
        super(database, Accessible.class);
    }

    // Helpers

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ExecutionResult describe() throws Exception {
        try (final Tx tx = beginTx()) {
            GraphQLSchema schema = new GraphQLImpl(api()).getSchema();
            tx.success();
            return new GraphQL(schema).execute(IntrospectionQuery.INTROSPECTION_QUERY);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ExecutionResult query(GraphQLQuery q) throws Exception {
        try (final Tx tx = beginTx()) {
            GraphQLSchema schema = new GraphQLImpl(api()).getSchema();
            ExecutionResult executionResult = new GraphQL(schema).execute(
                    q.getQuery(), (Object) null, q.getVariablesAsMap());
            tx.success();

            if (!executionResult.getErrors().isEmpty()) {
                throw new ExecutionError(executionResult.getErrors());
            }

            return executionResult;
        } catch (GraphQLException e) {
            // Hack: handle non validation of null variables by
            // returning a proper error...
            if (e.getMessage().contains("Null value")) {
                throw new ExecutionException(e);
            } else {
                throw e;
            }
        }
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    public ExecutionResult query(String q) throws Exception {
        return query(new GraphQLQuery(q));
    }
}
