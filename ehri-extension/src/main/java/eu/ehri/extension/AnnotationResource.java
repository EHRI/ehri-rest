package eu.ehri.extension;

import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import eu.ehri.project.exceptions.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.ListMultimap;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.Annotations;
import eu.ehri.project.views.impl.AnnotationViews;

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
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List all annotations.
     * 
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     */
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
     * Create a link between two items.
     * 
     * @param id
     * @param sourceId
     * @param json
     * @param accessors
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:.+}/{sourceId:.+}")
    public Response createAnnotationFor(@PathParam("id") String id,
            @PathParam("sourceId") String sourceId, String json,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Annotation ann = new AnnotationViews(graph).createLink(id,
                    sourceId, Bundle.fromString(json), user);
            new AclManager(graph).setAccessors(ann,
                    getAccessors(accessors, user));
            tx.success();
            return buildResponseFromAnnotation(ann);
        } catch (ItemNotFound e) {
            tx.failure();
            throw e;
        } catch (PermissionDenied e) {
            tx.failure();
            throw e;
        } catch (DeserializationError e) {
            tx.failure();
            throw e;
        } catch (BadRequester e) {
            tx.failure();
            throw e;
        } catch (ValidationError e) {
            tx.failure();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }    

    /**
     * Create an annotation for a particular item.
     * 
     * @param id
     * @param json
     * @param accessors
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:.+}")
    public Response createAnnotationFor(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Annotation ann = new AnnotationViews(graph).createFor(id,
                    Bundle.fromString(json), user);
            new AclManager(graph).setAccessors(ann,
                    getAccessors(accessors, user));
            tx.success();
            return buildResponseFromAnnotation(ann);
        } catch (ItemNotFound e) {
            tx.failure();
            throw e;
        } catch (PermissionDenied e) {
            tx.failure();
            throw e;
        } catch (DeserializationError e) {
            tx.failure();
            throw e;
        } catch (BadRequester e) {
            tx.failure();
            throw e;
        } catch (ValidationError e) {
            tx.failure();
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Return a map of annotations for the subtree of the given item and its
     * child items.
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
    public StreamingOutput listAnnotationsForSubtree(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, PermissionDenied {
        Annotations annotationViews = new AnnotationViews(graph);
        ListMultimap<String, Annotation> anns = annotationViews.getFor(id,
                getRequesterUserProfile());
        return streamingMultimap(anns);
    }

    private Response buildResponseFromAnnotation(Annotation ann)
            throws SerializationError {
        String jsonStr = serializer.vertexFrameToJson(ann);
        return Response.status(Status.CREATED).entity((jsonStr).getBytes())
                .build();
    }

    /**
     * Delete an annotation.
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteAgent(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
