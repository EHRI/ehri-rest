package eu.ehri.extension;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Action;

/**
 * Provides a RESTfull interface for the Action class. Note: Action instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path(EntityTypes.ACTION)
public class ActionResource extends EhriNeo4jFramedResource<Action> {

    public ActionResource(@Context GraphDatabaseService database) {
        super(database, Action.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getAction(@PathParam("id") long id) throws PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[^/]+}")
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listActions(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit) {
        return list(offset, limit);
    }
}
