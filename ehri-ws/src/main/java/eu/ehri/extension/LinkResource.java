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

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.DescriptionViews;
import eu.ehri.project.views.LinkViews;
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
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a web service interface for creating/reading item links.
 */
@Path(Entities.LINK)
public class LinkResource extends AbstractAccessibleResource<Link>
        implements GetResource, ListResource, DeleteResource, UpdateResource {

    public static final String BODY_PARAM = "body";

    private final LinkViews linkViews;
    private final DescriptionViews<Described> descriptionViews;

    public LinkResource(@Context GraphDatabaseService database) {
        super(database, Link.class);
        linkViews = new LinkViews(graph);
        descriptionViews = new DescriptionViews<>(graph, Described.class);
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
            throws PermissionDenied, ItemNotFound, ValidationError, DeserializationError {
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
     * @param bundle    The link body data
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
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile user = getCurrentUser();
            Link link = linkViews.create(targetId,
                    sourceId, bodies, bundle, user, getAccessors(accessors, user));
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
    @Path("accessPoint/{id:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor userProfile = getRequesterUserProfile();
            AccessPoint rel = manager.getEntity(id, AccessPoint.class);
            Description description = rel.getDescription();
            if (description == null) {
                throw new ItemNotFound(id);
            }
            Described item = description.getEntity();
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
    @Path("for/{id:.+}")
    public Response listRelatedItems(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            return streamingPage(getQuery(cls).page(
                    manager.getEntity(id, Linkable.class).getLinks(),
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
    @Path("for/{id:.+}/{linkId:.+}")
    public Response deleteLinkForItem(@PathParam("id") String id, @PathParam("linkId") String linkId)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            helper.checkEntityPermission(manager.getEntity(id, Accessible.class),
                    getRequesterUserProfile(), PermissionType.ANNOTATE);
            Actioner actioner = getRequesterUserProfile().as(Actioner.class);
            Link link = manager.getEntity(linkId, EntityClass.LINK, Link.class);
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
