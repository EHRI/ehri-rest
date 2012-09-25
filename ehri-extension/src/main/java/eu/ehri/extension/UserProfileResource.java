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

import eu.ehri.project.models.UserProfile;

/**
 * Provides a RESTfull interface for the UserProfile. 
 */
@Path("/ehri/userProfile")
public class UserProfileResource extends EhriNeo4jFramedResource<UserProfile> {

	public UserProfileResource(@Context GraphDatabaseService database) {
		super(database, UserProfile.class);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response getUserProfile(@PathParam("id") long id) {
		return retrieve(id);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("")
	public Response createUserProfile(String json) {
		return create(json);
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("")
	public Response updateUserProfile(String json) {
		return update(json);
	}
	
	@DELETE
	@Path("/{id}")
	public Response deleteUserProfile(@PathParam("id") long id) {
		return delete(id);
	}
}
