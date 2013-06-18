package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.LinkViews;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.ViewHelper;
import eu.ehri.project.views.impl.CrudViews;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Provides a RESTfull(ish) interface for creating/reading item links.
 */
@Path(Entities.LINK)
public class LinkResource extends
        AbstractAccessibleEntityResource<Link> {

    public static final String BODY_PARAM = "body";

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
     * Delete an access point.
     *
     * TODO: Move this elsewhere when there is a better access point API!!!
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     * @throws SerializationError
     */
    @DELETE
    @Path("/accessPoint/{id:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        Accessor userProfile = getRequesterUserProfile();
        UndeterminedRelationship rel = manager.getFrame(id, UndeterminedRelationship.class);
        Description description = rel.getDescription();
        if (description == null) {
            throw new ItemNotFound(id);
        }
        AccessibleEntity item = description.getEntity();
        if (item == null) {
            throw new ItemNotFound(id);
        }
        new CrudViews<AccessibleEntity>(graph, AccessibleEntity.class)
                .deleteDependent(rel, item, userProfile, UndeterminedRelationship.class);
        return Response.status(Status.OK).build();
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
     * Delete a link. If the optional ?accessPoint=[ID] parameter is also given
     * the access point associated with the link will also be deleted.
     *
     * @param id id of link to remove
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/for/{id:.+}/{linkId:.+}")
    public Response deleteLinkForItem(@PathParam("id") String id, @PathParam("linkId") String linkId)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        //return delete(id);
        // FIXME: Because it only takes ANNOTATE permissions to create a link
        // we need the same to delete them...
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            new ViewHelper(graph).checkEntityPermission(manager.getFrame(id, AccessibleEntity.class),
                    getRequesterUserProfile(), PermissionType.ANNOTATE);
            Actioner actioner = graph.frame(getRequesterUserProfile().asVertex(), Actioner.class);
            Link link = manager.getFrame(linkId, EntityClass.LINK, Link.class);
            new ActionManager(graph).logEvent(link, actioner, EventTypes.deletion);
            manager.deleteVertex(link.asVertex());
            tx.success();
            return Response.ok().build();
        } catch (ItemNotFound e) {
            tx.failure();
            throw e;
        } catch (PermissionDenied e) {
            tx.failure();
            throw e;
        } catch (BadRequester e) {
            tx.failure();
            throw e;        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Delete a link.
     *
     * @param id id of link to remove
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
