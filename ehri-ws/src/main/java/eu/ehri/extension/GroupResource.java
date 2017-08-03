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
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.Bundle;
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
import java.util.List;
import java.util.Set;

/**
 * Provides a web service interface for the Group model.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.GROUP)
public class GroupResource
        extends AbstractAccessibleResource<Group>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String MEMBER_PARAM = "member";

    public GroupResource(@Context GraphDatabaseService database) {
        super(database, Group.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response list() {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(MEMBER_PARAM) List<String> members)
            throws PermissionDenied, ValidationError, DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Api.Acl acl = api().acl();
            final Set<Accessor> groupMembers = Sets.newHashSet();
            for (String member : members) {
                groupMembers.add(manager.getEntity(member, Accessor.class));
            }
            Response item = createItem(bundle, accessors, group -> {
                for (Accessor member : groupMembers) {
                    acl.addAccessorToGroup(group, member);
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError, DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    /**
     * Add an accessor to a group.
     */
    @POST
    @Path("{id:[^/]+}/{aid:[^/]+}")
    public void addMember(@PathParam("id") String id, @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getEntity(aid, Accessor.class);
            api().acl().addAccessorToGroup(group, accessor);
            tx.success();
        }
    }

    /**
     * Remove an accessor from a group.
     */
    @DELETE
    @Path("{id:[^/]+}/{aid:[^/]+}")
    public void removeMember(@PathParam("id") String id, @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getEntity(aid, Accessor.class);
            api().acl().removeAccessorFromGroup(group, accessor);
            tx.success();
        }
    }

    /**
     * list members of the specified group;
     * UserProfiles and sub-Groups (direct descendants)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/list")
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Group group = manager.getEntity(id, EntityClass.GROUP, Group.class);
            Response response = streamingPage(() -> {
                Iterable<Accessible> members = all
                        ? group.getAllUserProfileMembers()
                        : group.getMembersAsEntities();
                return getQuery().page(members, Accessible.class);
            });
            tx.success();
            return response;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    /**
     * Fetch an aggregate list of a user's actions.
     *
     * @param userId      the user's ID
     * @param aggregation the manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    <b>strict</b>.
     * @return a list of event ranges
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/actions")
    public Response aggregateUserActions(
            @PathParam("id") String userId,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("strict") EventsApi.Aggregation aggregation)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Actioner group = manager.getEntity(userId, Actioner.class);
            EventsApi eventsApi = getEventsApi()
                    .withAggregation(aggregation);
            Response response = streamingListOfLists(() -> eventsApi.aggregateActions(group));
            tx.success();
            return response;
        }
    }
}
