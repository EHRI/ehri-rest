/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.extension;

import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.DescriptionViews;
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
 * Provides a web service interface for creating/reading item links.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.LINK)
public class LinkResource extends AbstractAccessibleEntityResource<Link>
        implements GetResource, ListResource, DeleteResource, UpdateResource {

    public static final String BODY_PARAM = "body";

    private final LinkViews linkViews;
    private final DescriptionViews<DescribedEntity> descriptionViews;

    public LinkResource(@Context GraphDatabaseService database) {
        super(database, Link.class);
        linkViews = new LinkViews(graph);
        descriptionViews = new DescriptionViews<>(graph, DescribedEntity.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response list() {
        return listItems();
    }

    @PUT
    @Path("{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ItemNotFound, ValidationError,DeserializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
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
            ItemNotFound, SerializationError {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Link link = linkViews.createLink(targetId,
                    sourceId, bodies, bundle, user);
            aclManager.setAccessors(link,
                    getAccessors(accessors, user));
            Response response = creationResponse(link);
            tx.success();
            return response;
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete an access point.
     */
    @DELETE
    @Path("/accessPoint/{id:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
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
            descriptionViews.delete(item.getId(), id, userProfile, getLogMessage());
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    /**
     * Returns a list of items linked to the given description.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public Response listRelatedItems(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Query<Link> linkQuery = new Query<>(graph, Link.class)
                .setStream(isStreaming());
            return streamingPage(linkQuery.setStream(isStreaming()).page(
                    manager.getFrame(id, LinkableEntity.class).getLinks(),
                    getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Delete a link. If the optional ?accessPoint=[ID] parameter is also given
     * the access point associated with the link will also be deleted.
     */
    @DELETE
    @Path("/for/{id:.+}/{linkId:.+}")
    public Response deleteLinkForItem(@PathParam("id") String id, @PathParam("linkId") String linkId)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            helper.checkEntityPermission(manager.getFrame(id, AccessibleEntity.class),
                    getRequesterUserProfile(), PermissionType.ANNOTATE);
            Actioner actioner = manager.cast(getRequesterUserProfile(), Actioner.class);
            Link link = manager.getFrame(linkId, EntityClass.LINK, Link.class);
            actionManager.newEventContext(link, actioner, EventTypes.deletion).commit();
            manager.deleteVertex(link.asVertex());
            tx.success();
            return Response.ok().build();
        }
    }

    /**
     * Delete a link.
     */
    @DELETE
    @Path("{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = deleteItem(id);
            tx.success();
            return item;
        }
    }
}
