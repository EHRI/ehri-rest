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
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.project.models.BasicFramedVertex;
import eu.ehri.project.models.EntityTypes;

/**
 * Just to see if it makes sense 
 *
 */
@Path(EntityTypes.BASIC)
public class BasicFramedVertexResource extends
		EhriNeo4jFramedResource<BasicFramedVertex> {

	public BasicFramedVertexResource(@Context GraphDatabaseService database) {
		super(database, BasicFramedVertex.class);
	}
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getBasicFramedVertex(@PathParam("id") long id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[\\w-]+}")
    public Response getBasicFramedVertex(@PathParam("id") String id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listBasicFramedVertexs() {
        return list();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response createBasicFramedVertex(String json) {
        return create(json);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response updateBasicFramedVertex(String json) {
        return update(json);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteBasicFramedVertex(@PathParam("id") long id) {
        return delete(id);
    }

}
