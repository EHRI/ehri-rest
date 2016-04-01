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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.models.base.Versioned;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.DescriptionViews;
import eu.ehri.project.views.EventViews;
import eu.ehri.project.views.PromotionViews;
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
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides a means of fetching items and lists of items
 * regardless of their specific type.
 */
@Path(GenericResource.ENDPOINT)
public class GenericResource extends AbstractAccessibleResource<Accessible> {

    public static final String ENDPOINT = "entities";
    public static final String ACCESS = "access";
    public static final String PROMOTE = "promote";
    public static final String DEMOTE = "demote";
    public static final String EVENTS = "events";
    public static final String ANNOTATIONS = "annotations";
    public static final String LINKS = "links";
    public static final String PERMISSION_GRANTS = "permission-grants";
    public static final String SCOPE_PERMISSION_GRANTS = "scope-permission-grants";
    public static final String DESCRIPTIONS = "descriptions";
    public static final String ACCESS_POINTS = "access-points";
    public static final String VERSIONS = "versions";

    private final PromotionViews pv;
    private final DescriptionViews<Described> dv;

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, Accessible.class);
        pv = new PromotionViews(graph);
        dv = new DescriptionViews<>(graph, Described.class);
    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param ids  a list of string IDs
     * @param gids a list of graph number IDs
     * @return A serialized list of items
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("id") List<String> ids,
            @QueryParam("gid") List<Long> gids) throws ItemNotFound {

        Tx tx = graph.getBaseGraph().beginTx();
        try {
            for (Long id : gids) {
                if (graph.getVertex(id) == null) {
                    throw new ItemNotFound(String.valueOf(id));
                }
            }

            FluentIterable<Vertex> verticesByGids = FluentIterable.from(gids)
                    .transform(new Function<Long, Vertex>() {
                        public Vertex apply(Long id) {
                            return graph.getVertex(id);
                        }
                    });

            // Object a lazily-computed view of the ids->vertices...
            Iterable<Vertex> verticesByIds = manager.getVertices(ids);
            Iterable<Vertex> allVertices = Iterables.concat(verticesByIds, verticesByGids);

            PipeFunction<Vertex, Boolean> filter = AclManager
                    .getAclFilterFunction(getRequesterUserProfile());
            GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(allVertices)
                    .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
            return streamingVertexList(filtered, getSerializer(), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param json a JSON-encoded list of IDs, which can consist of
     *             either strings or (if a number) graph (long) ids.
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listFromJson(String json)
            throws ItemNotFound, PermissionDenied, DeserializationError, IOException {
        IdSet set = parseGraphIds(json);
        return this.list(set.ids, set.gids);
    }

    /**
     * Fetch an item of any type by ID.
     *
     * @param id the item's ID
     * @return A serialized representation.
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    public Response get(@PathParam("id") String id) throws ItemNotFound, AccessDenied {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Vertex item = manager.getVertex(id);

            // If the item doesn't exist or isn't a content type throw 404
            Accessor currentUser = getRequesterUserProfile();
            if (item == null || !aclManager.getContentTypeFilterFunction().compute(item)) {
                throw new ItemNotFound(id);
            } else if (!AclManager.getAclFilterFunction(currentUser).compute(item)) {
                throw new AccessDenied(currentUser.getId(), id);
            }

            Response response = single(graph.frame(item, Accessible.class));
            tx.success();
            return response;
        }
    }

    /**
     * Get the accessors who are able to view an item.
     *
     * @param id the ID of the item
     * @return a list of accessor frames
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + ACCESS)
    public Response visibility(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, SerializationError {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessible item = manager.getEntity(id, Accessible.class);
            Iterable<Accessor> accessors = item.getAccessors();
            return streamingList(accessors, tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Set the accessors who are able to view an item. If no accessors
     * are set, the item is globally readable.
     *
     * @param id          the ID of the item
     * @param accessorIds the IDs of the users who can access this item.
     * @return the updated object
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + ACCESS)
    public Response setVisibility(@PathParam("id") String id,
            @QueryParam(ACCESSOR_PARAM) List<String> accessorIds)
            throws PermissionDenied, ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessible item = manager.getEntity(id, Accessible.class);
            Accessor current = getRequesterUserProfile();
            Set<Accessor> accessors = getAccessors(accessorIds, current);
            aclViews.setAccessors(item, accessors, current);
            Response response = single(item);
            tx.success();
            return response;
        }
    }

    /**
     * Up vote an item.
     *
     * @param id the ID of the item to promote.
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + PROMOTE)
    public Response addPromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getEntity(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.upVote(item, currentUser);
            Response response = single(item);
            tx.success();
            return response;
        } catch (PromotionViews.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove an up vote.
     *
     * @param id the ID of the item to remove
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + PROMOTE)
    public Response removePromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getEntity(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.removeUpVote(item, currentUser);
            Response response = single(item);
            tx.success();
            return response;
        }
    }

    /**
     * Down vote an item
     *
     * @param id the ID of the item to promote.
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DEMOTE)
    public Response addDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getEntity(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.downVote(item, currentUser);
            Response response = single(item);
            tx.success();
            return response;
        } catch (PromotionViews.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove a down vote.
     *
     * @param id the ID of the item to remove
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DEMOTE)
    public Response removeDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getEntity(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.removeDownVote(item, currentUser);
            Response response = single(item);
            tx.success();
            return response;
        }
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id          the event id
     * @param aggregation the aggregation strategy
     * @return A list of events
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + EVENTS)
    public Response events(
            @PathParam("id") String id,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventViews.Aggregation aggregation)
            throws ItemNotFound, AccessDenied {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            Accessible item = views
                    .setClass(Accessible.class).detail(id, accessor);
            EventViews eventViews = getEventViewsBuilder()
                    .withAggregation(aggregation)
                    .build();
            return streamingListOfLists(eventViews.aggregateForItem(item, accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Return a (paged) list of annotations for the given item.
     *
     * @param id the item ID
     * @return a list of annotations on the item
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + ANNOTATIONS)
    public Response annotations(
            @PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            return streamingPage(getQuery(Annotation.class).page(
                    manager.getEntity(id, Annotatable.class).getAnnotations(),
                    getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Returns a list of items linked to the given description.
     *
     * @param id the item ID
     * @return a list of links with the item as a target
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + LINKS)
    public Response links(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            return streamingPage(getQuery(Link.class).page(
                    manager.getEntity(id, Linkable.class).getLinks(),
                    getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return a list of grants for this item
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + PERMISSION_GRANTS)
    public Response permissionGrants(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            PermissionGrantTarget target = manager.getEntity(id, PermissionGrantTarget.class);
            Accessor accessor = getRequesterUserProfile();
            return streamingPage(getQuery(PermissionGrant.class)
                    .page(target.getPermissionGrants(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return a list of grants for which this item is the scope
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + SCOPE_PERMISSION_GRANTS)
    public Response permissionGrantsAsScope(@PathParam("id") String id)
            throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            PermissionScope scope = manager.getEntity(id, PermissionScope.class);
            Accessor accessor = getRequesterUserProfile();
            return streamingPage(getQuery(PermissionGrant.class)
                    .page(scope.getPermissionGrants(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Create a new description for this item.
     *
     * @param id     the item ID
     * @param bundle the description bundle
     * @return the new description
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DESCRIPTIONS)
    public Response createDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Described item = views.setClass(Described.class).detail(id, user);
            Description desc = dv.create(id, bundle,
                    Description.class, user, getLogMessage());
            item.addDescription(desc);
            Response response = buildResponse(item, desc, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Update a description belonging to the given item.
     *
     * @param id     the item ID
     * @param bundle the description bundle
     * @return the new description
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DESCRIPTIONS)
    public Response updateDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor userProfile = getRequesterUserProfile();
            Described item = views.setClass(Described.class).detail(id, userProfile);
            Mutation<Description> desc = dv.update(id, bundle,
                    Description.class, userProfile, getLogMessage());
            Response response = buildResponse(item, desc.getNode(), Response.Status.OK);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Update a description with the given ID, belonging to the given parent item.
     *
     * @param id     the item ID
     * @param did    the description ID
     * @param bundle the description bundle
     * @return the new description
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DESCRIPTIONS + "/{did:[^/]+}")
    public Response updateDescriptionWithId(@PathParam("id") String id,
            @PathParam("did") String did, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, SerializationError {
        return updateDescription(id, bundle.withId(did));
    }

    /**
     * Delete a description with the given ID, belonging to the given parent item.
     *
     * @param id  the item ID
     * @param did the description ID
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws SerializationError
     */
    @DELETE
    @Path("{id:[^/]+}/" + DESCRIPTIONS + "/{did:[^/]+}")
    public void deleteDescription(
            @PathParam("id") String id, @PathParam("did") String did)
            throws PermissionDenied, ItemNotFound, ValidationError, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            dv.delete(id, did, user, getLogMessage());
            tx.success();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Create an access point on the given description, for the given
     * parent item.
     *
     * @param id     the parent item's ID
     * @param did    the description's ID
     * @param bundle the access point data
     * @return the new access point
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + DESCRIPTIONS + "/{did:[^/]+}/" + ACCESS_POINTS)
    public Response createAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Described item = views.setClass(Described.class).detail(id, user);
            Description desc = manager.getEntity(did, Description.class);
            AccessPoint rel = dv.create(id, bundle,
                    AccessPoint.class, user, getLogMessage());
            desc.addAccessPoint(rel);
            Response response = buildResponse(item, rel, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }


    /**
     * Lookup and page the versions for a given item.
     *
     * @param id the event id
     * @return a list of versions
     * @throws ItemNotFound
     * @throws AccessDenied
     */
    @GET
    @Path("{id:[^/]+}/" + VERSIONS)
    public Response listFor(@PathParam("id") String id) throws ItemNotFound, AccessDenied {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Versioned item = views.setClass(Versioned.class)
                    .detail(id, user);
            return streamingPage(getQuery(Version.class).setStream(true)
                    .page(item.getAllPriorVersions(), user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Delete the access point with the given ID, on the given description,
     * belonging to the given parent item.
     *
     * @param id   the parent item's ID
     * @param did  the description's ID
     * @param apid the access point's ID
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    @DELETE
    @Path("{id:[^/]+}/" + DESCRIPTIONS + "/{did:[^/]+}/" + ACCESS_POINTS + "/{apid:[^/]+}")
    public void deleteAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, @PathParam("apid") String apid)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile user = getCurrentUser();
            dv.delete(id, apid, user, getLogMessage());
            tx.success();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    // Helpers

    private Response buildResponse(Described item, Entity data, Response.Status status)
            throws SerializationError {
        return Response.status(status).location(getItemUri(item))
                .entity((getSerializer().entityToJson(data))
                        .getBytes(Charsets.UTF_8)).build();
    }

    private static class IdSet {
        public final List<String> ids;
        public final List<Long> gids;

        public IdSet(List<String> ids, List<Long> gids) {
            this.ids = ids;
            this.gids = gids;
        }
    }

    private IdSet parseGraphIds(String json) throws IOException, DeserializationError {
        try {
            TypeReference<List<Object>> typeRef = new TypeReference<List<Object>>() {
            };
            List<Object> jsonValues = jsonMapper.readValue(json, typeRef);
            List<String> ids = Lists.newArrayList();
            List<Long> gids = Lists.newArrayList();

            for (Object js : jsonValues) {
                if (js instanceof Integer) {
                    gids.add(Long.valueOf((Integer) js));
                } else if (js instanceof Long) {
                    gids.add((Long) js);
                } else if (js instanceof String) {
                    ids.add((String) js);
                }
            }
            return new IdSet(ids, gids);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}
