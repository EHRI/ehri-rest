package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.ContentType;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a RESTful interface for the ContentType class. Note: ContentType instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
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
    public Response listContentTypes(
            @QueryParam(PAGE_PARAM) @DefaultValue("1") int page,
            @QueryParam(COUNT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int count,
            @QueryParam(SORT_PARAM) List<String> order,            
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(page, count, order, filters);
    }
}
