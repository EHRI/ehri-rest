package eu.ehri.extension;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

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
@Path(EntityTypes.PERMISSION)
public class Permissions extends EhriNeo4jFramedResource<Action> {

    public Permissions(@Context GraphDatabaseService database) {
        super(database, Action.class);
    }

    @POST
    @Path("/set")
    public Response getAction(
    		@QueryParam("ctype") Set<String> ctypes,
    		@QueryParam("perm") Set<Boolean> perms    		
    		) throws PermissionDenied {
    	System.out.println("CTYPES: " + ctypes);
    	System.out.println("PERMSS: " + perms);
    	String out = String.format("%s\n%s\n", ctypes, perms);
    	return Response.status(Status.OK)
                .entity((out).getBytes()).build();
    }
}
