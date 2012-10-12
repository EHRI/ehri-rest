package eu.ehri.extension;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;

/**
 * Provides a RESTfull interface for the UserProfile.
 */
@Path(EntityTypes.USER_PROFILE)
public class UserProfileResource extends EhriNeo4jFramedResource<UserProfile> {

    public UserProfileResource(@Context GraphDatabaseService database) {
        super(database, UserProfile.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProfile(@QueryParam("key") String key,
            @QueryParam("value") String value) throws ItemNotFound,
            PermissionDenied {
        return retrieve(key, value);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getUserProfile(@PathParam("id") long id)
            throws PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[\\w-]+}")
    public Response getUserProfile(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listUserProfiles() throws PermissionDenied {
        return list();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUserProfile(String json) throws PermissionDenied,
            ValidationError, IntegrityError {
        return create(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserProfile(String json) throws PermissionDenied,
            IntegrityError, ValidationError {
        return update(json);
    }

    @DELETE
    @Path("/{id:\\d+}")
    public Response deleteUserProfile(@PathParam("id") long id)
            throws PermissionDenied, ValidationError {
        return delete(id);
    }

    @DELETE
    @Path("/{id:[\\w-]+}")
    public Response deleteUserProfile(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        return delete(id);
    }
}
