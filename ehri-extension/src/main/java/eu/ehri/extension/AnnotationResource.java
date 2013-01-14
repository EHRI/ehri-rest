package eu.ehri.extension;

import java.util.List;

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

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull(ish) interface for creating.
 */
@Path(Entities.ANNOTATION)
public class AnnotationResource extends
        AbstractAccessibleEntityResource<Annotation> {

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
    }

    /**
     * Retrieve an annotation by id.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listAnnotations(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,            
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    /**
     * List the annotations for a given item.
     * 
     * FIXME: Ultimately this method will have to be more clever, and fetch the
     * annotations for an entire subtree, but it's not for now.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/for/{id:.+}")
    public StreamingOutput listActionsForItem(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, PermissionDenied {
        new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(
                manager.getFrame(id, AccessibleEntity.class),
                getRequesterUserProfile());
        final Iterable<Annotation> list = new Query<Annotation>(graph,
                Annotation.class).list(
                manager.getFrame(id, AnnotatableEntity.class).getAnnotations(),
                getRequesterUserProfile());
        return streamingList(list);
    }

}
