package eu.ehri.extension;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityTypes;

/**
 * Provides a RESTfull interface for the Action class. Note: Action instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path(EntityTypes.CONTENT_TYPE)
public class ContentTypeResource extends EhriNeo4jFramedResource<ContentType> {

    public ContentTypeResource(@Context GraphDatabaseService database) {
        super(database, ContentType.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getContentType(@PathParam("id") long id) throws PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[\\w-]+}")
    public Response getContentType(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listContentTypes() throws PermissionDenied {
        return list();
    }
}
