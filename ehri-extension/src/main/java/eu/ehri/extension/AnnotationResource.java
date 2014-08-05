package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.AnnotationViews;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.impl.CrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a RESTful(ish) interface for creating.
 */
@Path(Entities.ANNOTATION)
public class AnnotationResource extends
        AbstractAccessibleEntityResource<Annotation> {

    private final AnnotationViews annotationViews;

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
        annotationViews = new AnnotationViews(graph);
    }

    /**
     * Retrieve an annotation by id.
     *
     * @param id The item's id
     * @return The serialized annotation
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List all annotations.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listAnnotations() throws ItemNotFound, BadRequester {
        return page();
    }

    /**
     * Create an annotation for a particular item.
     *
     * @param id        The ID of the item being annotation
     * @param bundle      The JSON representation of the annotation
     * @param accessors User IDs who can access the annotation
     * @return The annotation
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response createAnnotationFor(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, AccessDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            Annotation ann = annotationViews.createFor(id, id,
                    bundle, user, getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return creationResponse(ann);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create an annotation for a dependent node on a given item.
     *
     * @param id        The ID of the item being annotation
     * @param did       The ID of the description being annotated
     * @param bundle      The JSON representation of the annotation
     * @param accessors User IDs who can access the annotation
     * @return The annotation
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/{did:.+}")
    public Response createAnnotationFor(
            @PathParam("id") String id,
            @PathParam("did") String did,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, AccessDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            Annotation ann = annotationViews.createFor(id, did,
                    bundle, user, getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return creationResponse(ann);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Return a map of annotations for the subtree of the given item and its
     * child items.
     *
     * @param id The item ID
     * @return A list of annotations on the item and it's dependent children.
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public Response listAnnotationsForSubtree(
            @PathParam("id") String id,
            @QueryParam(PAGE_PARAM) @DefaultValue("1") int page,
            @QueryParam(COUNT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int count,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws AccessDenied, ItemNotFound, BadRequester, PermissionDenied {
        AccessibleEntity item = new CrudViews<AccessibleEntity>(graph, AccessibleEntity.class)
                .detail(id, getRequesterUserProfile());
        Query<Annotation> query = new Query<Annotation>(graph, cls)
                .setPage(page).setCount(count).filter(filters)
                .orderBy(order).filter(filters)
                .setStream(isStreaming());
        return streamingPage(query.page(item.getAnnotations(), getRequesterUserProfile()));

    }

    /**
     * Update an annotation.
     *
     * @param id The annotation ID
     * @return The updated item
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @PUT
    @Path("/{id:.+}")
    public Response updateAnnotation(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, DeserializationError, IntegrityError {
        return update(id, json);
    }

    /**
     * Delete an annotation.
     *
     * @param id The annotation ID
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteAnnotation(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
