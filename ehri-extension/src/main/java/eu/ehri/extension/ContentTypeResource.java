package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.ContentType;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful interface for the ContentType class. Note: ContentType instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.CONTENT_TYPE)
public class ContentTypeResource extends AbstractAccessibleEntityResource<ContentType> {

    public ContentTypeResource(@Context GraphDatabaseService database) {
        super(database, ContentType.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getContentType(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listContentTypes() throws ItemNotFound, BadRequester {
        return page();
    }
}
