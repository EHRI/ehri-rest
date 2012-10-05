package eu.ehri.extension;

import java.net.URI;

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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.views.ActionViews;

/**
 * Provides a RESTfull interface for the Agent
 */
@Path(EntityTypes.AGENT)
public class AgentResource extends EhriNeo4jFramedResource<Agent> {

    public AgentResource(@Context GraphDatabaseService database) {
        super(database, Agent.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getAgent(@PathParam("id") long id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[\\w-]+}")
    public Response getAgent(@PathParam("id") String id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listAgents() {
        return list();
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

    /**
     * Create an instance of the 'entity' in the database
     * 
     * @param json
     *            The json representation of the entity to create (no vertex
     *            'id' fields)
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response createAgentDocumentaryUnit(@PathParam("id") long id,
            String json) {

        try {
            Agent agent = views.detail(id, getRequesterUserProfileId());
            EntityBundle<DocumentaryUnit> entityBundle = converter.jsonToBundle(json);
            
            DocumentaryUnit doc = new ActionViews<DocumentaryUnit>(graph, DocumentaryUnit.class)
                    .create(converter.bundleToData(entityBundle),
                            getRequesterUserProfileId());
            // Add it to this agent's collections
            agent.addCollection(doc);
            
            String jsonStr = converter.vertexFrameToJson(doc);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI docUri = ub.path(doc.asVertex().getId().toString()).build();
            
            return Response.status(Status.OK).location(docUri)
                    .entity((jsonStr).getBytes()).build();
            
        } catch (PermissionDenied e) {
            return Response.status(Status.UNAUTHORIZED)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        } catch (ValidationError e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        } catch (SerializationError e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        } catch (DeserializationError e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }
}
