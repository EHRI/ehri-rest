package eu.ehri.extension;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.project.models.Agent;

/**
 * Provides a RESTfull interface for the Agent 
 */
@Path("/ehri/agent")
public class AgentResource extends EhriNeo4jFramedResource<Agent> {

    public AgentResource(@Context GraphDatabaseService database) {
        super(database, Agent.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response getAgent(@PathParam("id") long id) {
        return retrieve(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response createAgent(String json) {
        return create(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response updateAgent(String json) {
        return update(json);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAgent(@PathParam("id") long id) {
        return delete(id);
    }
}
