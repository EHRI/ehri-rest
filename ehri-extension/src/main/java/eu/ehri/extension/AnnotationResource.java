package eu.ehri.extension;

import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.AnnotationViews;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.impl.CrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

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
import java.util.List;

/**
 * Provides a RESTful(ish) interface for creating.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.ANNOTATION)
public class AnnotationResource
        extends AbstractAccessibleEntityResource<Annotation>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    private final AnnotationViews annotationViews;

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
        annotationViews = new AnnotationViews(graph);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    @Override
    public Response list() throws BadRequester {
        return listItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    @Override
    public long count() throws BadRequester {
        return countItems();
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
     * child items. Standard list parameters apply.
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
            @PathParam("id") String id)
            throws AccessDenied, ItemNotFound, BadRequester, PermissionDenied {
        AccessibleEntity item = new CrudViews<AccessibleEntity>(graph, AccessibleEntity.class)
                .detail(id, getRequesterUserProfile());
        Query<Annotation> query = getQuery(Annotation.class)
                .setStream(isStreaming());
        return streamingPage(query.page(item.getAnnotations(), getRequesterUserProfile()));

    }

    @PUT
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, DeserializationError {
        return updateItem(id, bundle);
    }

    @PUT
    @Override
    public Response update(Bundle bundle)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, DeserializationError {
        return updateItem(bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return deleteItem(id);
    }
}
