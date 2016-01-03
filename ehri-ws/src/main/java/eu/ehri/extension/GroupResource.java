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

import com.google.common.collect.Sets;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Set;

/**
 * Provides a web service interface for the Group model.
 */
@Path(Entities.GROUP)
public class GroupResource
        extends AbstractAccessibleResource<Group>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String MEMBER_PARAM = "member";

    public GroupResource(@Context GraphDatabaseService database) {
        super(database, Group.class);
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
    @Override
    public Response list() {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createGroup(Bundle bundle,
                                @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                @QueryParam(MEMBER_PARAM) List<String> members)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            final UserProfile currentUser = getCurrentUser();
            final Set<Accessor> groupMembers = Sets.newHashSet();
            for (String member : members) {
                groupMembers.add(manager.getEntity(member, Accessor.class));
            }
            Response item = createItem(bundle, accessors, new Handler<Group>() {
                @Override
                public void process(Group group) throws PermissionDenied {
                    for (Accessor member : groupMembers) {
                        aclViews.addAccessorToGroup(group, member, currentUser);
                    }
                }
            });
            tx.success();
            return item;
        } catch (ItemNotFound e) {
            throw new DeserializationError("User or group not found: " + e.getValue());
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    /**
     * Add an accessor to a group.
     */
    @POST
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response addMember(@PathParam("id") String id,
                              @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getEntity(aid, Accessor.class);
            aclViews.addAccessorToGroup(group, accessor, getRequesterUserProfile());
            Response response = Response.status(Status.OK).location(getItemUri(accessor)).build();
            tx.success();
            return response;
        }
    }

    /**
     * Remove an accessor from a group.
     */
    @DELETE
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response removeMember(@PathParam("id") String id,
                                 @PathParam("aid") String aid) throws PermissionDenied,
            ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getEntity(aid, Accessor.class);

            new AclViews(graph).removeAccessorFromGroup(group, accessor, getRequesterUserProfile());
            Response response = Response.status(Status.OK).location(getItemUri(accessor)).build();
            tx.success();
            return response;
        }
    }

    /**
     * list members of the specified group;
     * UserProfiles and sub-Groups (direct descendants)
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:[^/]+}/list")
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Iterable<Accessible> members = all
                    ? group.getAllUserProfileMembers()
                    : group.getMembersAsEntities();
            return streamingPage(getQuery(Accessible.class)
                    .page(members, getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public long countChildResources(@PathParam("id") String id,
                                    @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Group group = views.detail(id, user);
            Iterable<Accessible> members = all
                    ? group.getAllUserProfileMembers()
                    : group.getMembersAsEntities();
            long count = getQuery(Accessible.class)
                    .count(members);
            tx.success();
            return count;
        }
    }

    /**
     * Delete a group with the given identifier string.
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
