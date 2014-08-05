package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.views.LinkViews;
import eu.ehri.project.views.Query;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a RESTful(ish) interface for creating/reading item links.
 */
@Path(Entities.LINK)
public class LinkResource extends
        AbstractAccessibleEntityResource<Link> {

    public static final String BODY_PARAM = "body";

    private LinkViews linkViews;

    public LinkResource(@Context GraphDatabaseService database) {
        super(database, Link.class);
        linkViews = new LinkViews(graph);
    }

    /**
     * Retrieve an annotation by id.
     *
     * @param id The annotation ID
     * @return The annotation
     * @throws ItemNotFound
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
    public Response listLinks() throws ItemNotFound, BadRequester {
        return page();
    }

    /**
     * Update a link,
     */
    @PUT
    @Path("/{id:.+}")
    public Response updateLink(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, DeserializationError, IntegrityError {
        return update(id, json);
    }

    /**
     * Create a link between two items.
     *
     * @param targetId  The link target
     * @param sourceId  The link source
     * @param bundle      The link body data
     * @param bodies    optional list of entities to provide the body
     * @param accessors The IDs of accessors who can see this link
     * @return The created link item
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
    @Path("{targetId:.+}/{sourceId:.+}")
    public Response createLinkFor(
            @PathParam("targetId") String targetId,
            @PathParam("sourceId") String sourceId, Bundle bundle,
            @QueryParam(BODY_PARAM) List<String> bodies,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError,
            ItemNotFound, BadRequester, SerializationError {
        Accessor user = getRequesterUserProfile();
        try {
            Link link = linkViews.createLink(targetId,
                    sourceId, bodies, bundle, user);
            aclManager.setAccessors(link,
                    getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return creationResponse(link);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            if (graph.getBaseGraph().isInTransaction()) {
                graph.getBaseGraph().rollback();
            }
        }

    }

    /**
     * Delete an access point.
     */
    @DELETE
    @Path("/accessPoint/{id:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor userProfile = getRequesterUserProfile();
            UndeterminedRelationship rel = manager.getFrame(id, UndeterminedRelationship.class);
            Description description = rel.getDescription();
            if (description == null) {
                throw new ItemNotFound(id);
            }
            DescribedEntity item = description.getEntity();
            if (item == null) {
                throw new ItemNotFound(id);
            }

            helper.checkEntityPermission(item, userProfile, PermissionType.UPDATE);

            // FIXME: No logging here?
            new BundleDAO(graph).delete(getSerializer().vertexFrameToBundle(rel));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Returns a list of items linked to the given description.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public Response listRelatedItems(@PathParam("id") String id)
            throws ItemNotFound, BadRequester {
        Query<Link> linkQuery = new Query<Link>(graph, Link.class)
                .setStream(isStreaming());
        return streamingPage(linkQuery.setStream(isStreaming()).page(
                manager.getFrame(id, LinkableEntity.class).getLinks(),
                getRequesterUserProfile()));
    }

    /**
     * Delete a link. If the optional ?accessPoint=[ID] parameter is also given
     * the access point associated with the link will also be deleted.
     */
    @DELETE
    @Path("/for/{id:.+}/{linkId:.+}")
    public Response deleteLinkForItem(@PathParam("id") String id, @PathParam("linkId") String linkId)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            helper.checkEntityPermission(manager.getFrame(id, AccessibleEntity.class),
                    getRequesterUserProfile(), PermissionType.ANNOTATE);
            Actioner actioner = graph.frame(getRequesterUserProfile().asVertex(), Actioner.class);
            Link link = manager.getFrame(linkId, EntityClass.LINK, Link.class);
            new ActionManager(graph).logEvent(link, actioner, EventTypes.deletion);
            manager.deleteVertex(link.asVertex());
            graph.getBaseGraph().commit();
            return Response.ok().build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Delete a link.
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteLink(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
