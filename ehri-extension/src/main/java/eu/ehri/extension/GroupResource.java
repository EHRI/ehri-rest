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
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import java.util.Set;

/**
 * Provides a web service interface for the Group model.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.GROUP)
public class GroupResource
        extends AbstractAccessibleEntityResource<Group>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String MEMBER_PARAM = "member";

    public GroupResource(@Context GraphDatabaseService database) {
        super(database, Group.class);
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createGroup(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(MEMBER_PARAM) List<String> members)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        final UserProfile currentUser = getCurrentUser();
        try {
            final Set<Accessor> groupMembers = Sets.newHashSet();
            for (String member : members) {
                groupMembers.add(manager.getFrame(member, Accessor.class));
            }
            return createItem(bundle, accessors, new Handler<Group>() {
                @Override
                public void process(Group group) throws PermissionDenied {
                    for (Accessor member : groupMembers) {
                        aclViews.addAccessorToGroup(group, member, currentUser);
                    }
                }
            });
        } catch (ItemNotFound e) {
            graph.getBaseGraph().rollback();
            throw new DeserializationError("User or group not found: " + e.getValue());
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response update(Bundle bundle) throws PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return updateItem(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return updateItem(id, bundle);
    }

    /**
     * Add an accessor to a group.
     */
    @POST
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response addMember(@PathParam("id") String id,
            @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Group group = manager.getFrame(id, EntityClass.GROUP, Group.class);
        Accessor accessor = manager.getFrame(aid, Accessor.class);
        try {
            aclViews.addAccessorToGroup(group, accessor, getRequesterUserProfile());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).location(getItemUri(accessor)).build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Remove an accessor from a group.
     */
    @DELETE
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response removeMember(@PathParam("id") String id,
            @PathParam("aid") String aid) throws PermissionDenied,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Group group = manager.getFrame(id, EntityClass.GROUP, Group.class);
        Accessor accessor = manager.getFrame(aid, Accessor.class);
        try {
            new AclViews(graph).removeAccessorFromGroup(group, accessor, getRequesterUserProfile());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).location(getItemUri(accessor)).build();
        } finally {
            cleanupTransaction();
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
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Group group = manager.getFrame(id, EntityClass.GROUP, Group.class);
        Iterable<AccessibleEntity> members = all
                ? group.getAllUserProfileMembers()
                : group.getMembersAsEntities();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(members, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public long countChildResources(@PathParam("id") String id,
                @QueryParam(ALL_PARAM) @DefaultValue("false")  boolean all)
            throws ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Group group = views.detail(id, user);
        Iterable<AccessibleEntity> members = all
                ? group.getAllUserProfileMembers()
                : group.getMembersAsEntities();
        return getQuery(AccessibleEntity.class)
                .count(members);
    }

    /**
     * Delete a group with the given identifier string.
     */
    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return deleteItem(id);
    }
}
