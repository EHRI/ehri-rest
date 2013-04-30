package eu.ehri.extension;

import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.LinkManager;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.views.AnnotationViews;
import eu.ehri.project.views.LinkViews;
import eu.ehri.project.views.Query;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;
import java.util.Map;

/**
 * Provides a RESTfull(ish) interface for creating/reading item links.
 */
@Path(Entities.LINK)
public class LinkResource extends
        AbstractAccessibleEntityResource<Link> {

    public static final String BODY_PARAM = "body";
    public static final String BODY_NAME = "bodyName";
    public static final String BODY_TYPE = "bodyType";

    public LinkResource(@Context GraphDatabaseService database) {
        super(database, Link.class);
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
    public StreamingOutput listLinks(
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
     * @param bodies optional list of entities to provide the body
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
    public Response createLinkFor(@PathParam("id") String id,
            @PathParam("sourceId") String sourceId, String json,
            @QueryParam(BODY_PARAM) List<String> bodies,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Link link = new LinkViews(graph).createLink(id,
                    sourceId, bodies, Bundle.fromString(json), user);
            new AclManager(graph).setAccessors(link,
                    getAccessors(accessors, user));
            tx.success();
            return buildResponseFromAnnotation(link);
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
     * Create a link between two items.
     *
     * @param id
     * @param targetId
     * @param descriptionId the description to add the access point to.
     * @param json  the link data
     * @param bodyName name of the access point to create.
     * @param bodyType type of the access point to create.
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
    @Path("{id:.+}/{targetId:.+}/{descriptionId:.+}")
    public Response createAccessPointLinkFor(@PathParam("id") String id,
            @PathParam("targetId") String targetId, @PathParam("descriptionId") String descriptionId,
            String json,
            @QueryParam(BODY_NAME) String bodyName,
            @QueryParam(BODY_TYPE) String bodyType,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Link link = new LinkViews(graph).createAccessPointLink(id,
                    targetId, descriptionId, bodyName, bodyType, Bundle.fromString(json), user);
            new AclManager(graph).setAccessors(link,
                    getAccessors(accessors, user));
            tx.success();
            return buildResponseFromAnnotation(link);
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

    private Response buildResponseFromAnnotation(Link link)
            throws SerializationError {
        String jsonStr = serializer.vertexFrameToJson(link);
        return Response.status(Status.CREATED).entity((jsonStr).getBytes())
                .build();
    }

    /**
     * Returns a list of items linked to the given description.
     *
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/for/{id:.+}")
    public StreamingOutput listRelatedItems(@PathParam("id") String id)
                throws ItemNotFound, BadRequester {
        Query<Link> linkQuery = new Query<Link>(graph, Link.class);
        return streamingList(linkQuery.list(
                manager.getFrame(id, LinkableEntity.class).getLinks(),
                getRequesterUserProfile()));
    }

    /**
     * Delete a link.
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteLink(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
