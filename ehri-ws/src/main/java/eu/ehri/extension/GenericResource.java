/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.impl.ApiImpl;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.exporters.dc.DublinCore11Exporter;
import eu.ehri.project.exporters.dc.DublinCoreExporter;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Versioned;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.w3c.dom.Document;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import java.util.function.Function;

/**
 * Provides a means of fetching items and lists of items
 * regardless of their specific type.
 */
@Path(GenericResource.ENDPOINT)
public class GenericResource extends AbstractAccessibleResource<Accessible> {

    public static final String ENDPOINT = "entities";

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, Accessible.class);
    }

    /**
     * Fetch a list of items by their ID.
     * <p>
     * Note: if <i>both</i> global IDs and graph IDs (GIDs) are given as query
     * parameters, GIDs will be returned at the head of the response list.
     *
     * @param ids  a list of string IDs
     * @param gids a list of graph number IDs
     * @return A serialized list of items, with nulls for items that are not
     * found or inaccessible.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("id") List<String> ids, @QueryParam("gid") List<Long> gids) {
        try (Tx tx = beginTx()) {
            PipeFunction<Vertex, Boolean> aclFilter = AclManager
                    .getAclFilterFunction(getRequesterUserProfile());
            PipeFunction<Vertex, Boolean> contentFilter = aclManager
                    .getContentTypeFilterFunction();
            Function<Vertex, Vertex> filter = v ->
                    (contentFilter.compute(v) && aclFilter.compute(v)) ? v : null;

            Iterable<Vertex> byGid = Iterables.transform(gids, graph::getVertex);
            Iterable<Vertex> byId = manager.getVertices(ids);
            Iterable<Vertex> all = Iterables.concat(byGid, byId);
            Response response = streamingVertexList(() -> Iterables.transform(all, filter::apply));
            tx.success();
            return response;
        }
    }

    /**
     * Fetch a list of items by their ID.
     * <p>
     * Note: if <i>both</i> global IDs and graph IDs (GIDs) are given as query
     * parameters, GIDs will be returned at the head of the response list.
     *
     * @param json a JSON-encoded list of IDs, which can consist of
     *             either strings or (if a number) graph (long) ids.
     * @return A serialized list of items, with nulls for items that are not
     * found or inaccessible.
     * @throws DeserializationError if the input JSON is not well-formed
     * @throws IOException          if the input cannot be read
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listFromJson(String json) throws DeserializationError, IOException {
        IdSet set = parseGraphIds(json);
        return this.list(set.ids, set.gids);
    }

    /**
     * Fetch an item of any type by ID.
     *
     * @param id the item's ID
     * @return A serialized representation.
     * @throws ItemNotFound if the item does not exist
     * @throws AccessDenied if the user cannot access the item
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    public Response get(@PathParam("id") String id) throws ItemNotFound, AccessDenied {
        try (final Tx tx = beginTx()) {
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
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/access")
    public Response visibility(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessible item = manager.getEntity(id, Accessible.class);
            Iterable<Accessor> accessors = item.getAccessors();
            Response response = streamingList(() -> accessors);
            tx.success();
            return response;
        }
    }

    /**
     * Set the accessors who are able to view an item. If no accessors
     * are set, the item is globally readable.
     *
     * @param id          the ID of the item
     * @param accessorIds the IDs of the users who can access this item.
     * @return the updated object
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ItemNotFound     if the item does not exist
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/access")
    public Response setVisibility(@PathParam("id") String id,
                                  @QueryParam(ACCESSOR_PARAM) List<String> accessorIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessible item = manager.getEntity(id, Accessible.class);
            Accessor current = getRequesterUserProfile();
            Set<Accessor> accessors = getAccessors(accessorIds, current);
            api().acl().setAccessors(item, accessors);
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
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ItemNotFound     if the item does not exist
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/promote")
    public Response addPromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().promote(id));
            tx.success();
            return item;
        } catch (ApiImpl.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove an up vote.
     *
     * @param id the ID of the item to remove
     * @return 200 response
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ItemNotFound     if the item does not exist
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/promote")
    public Response removePromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response response = single(api().removePromotion(id));
            tx.success();
            return response;
        }
    }

    /**
     * Down vote an item
     *
     * @param id the ID of the item to promote.
     * @return 200 response
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ItemNotFound     if the item does not exist
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/demote")
    public Response addDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().demote(id));
            tx.success();
            return item;
        } catch (ApiImpl.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove a down vote.
     *
     * @param id the ID of the item to remove
     * @return 200 response
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ItemNotFound     if the item does not exist
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/demote")
    public Response removeDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().removeDemotion(id));
            tx.success();
            return item;
        }
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id          the event id
     * @param aggregation the aggregation strategy
     * @return A list of events
     * @throws ItemNotFound if the item does not exist
     * @throws AccessDenied if the user cannot access the item
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/events")
    public Response events(
            @PathParam("id") String id,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventsApi.Aggregation aggregation)
            throws ItemNotFound, AccessDenied {
        try (final Tx tx = beginTx()) {
            Accessible item = manager.getEntity(id, Accessible.class);
            EventsApi eventsApi = getEventsApi()
                    .withAggregation(aggregation);
            Response response = streamingListOfLists(() -> eventsApi.aggregateForItem(item));
            tx.success();
            return response;
        }
    }

    /**
     * Return a (paged) list of annotations for the given item.
     *
     * @param id the item ID
     * @return a list of annotations on the item
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/annotations")
    public Response annotations(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Annotatable entity = manager.getEntity(id, Annotatable.class);
            Response response = streamingPage(() -> getQuery().page(
                    entity.getAnnotations(),
                    Annotation.class));
            tx.success();
            return response;
        }
    }

    /**
     * Returns a list of items linked to the given description.
     *
     * @param id the item ID
     * @return a list of links with the item as a target
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/links")
    public Response links(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Linkable entity = manager.getEntity(id, Linkable.class);
            Response response = streamingPage(() -> getQuery().page(
                    entity.getLinks(),
                    Link.class));
            tx.success();
            return response;
        }
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @param id the item ID
     * @return a list of grants for this item
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/permission-grants")
    public Response permissionGrants(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            PermissionGrantTarget target = manager.getEntity(id, PermissionGrantTarget.class);
            Response response = streamingPage(() -> getQuery()
                    .page(target.getPermissionGrants(),
                            PermissionGrant.class));
            tx.success();
            return response;
        }
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @param id the item ID
     * @return a list of grants for which this item is the scope
     * @throws ItemNotFound if one of the given items does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/scope-permission-grants")
    public Response permissionGrantsAsScope(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            PermissionScope scope = manager.getEntity(id, PermissionScope.class);
            Response response = streamingPage(() -> getQuery()
                    .page(scope.getPermissionGrants(),
                            PermissionGrant.class));
            tx.success();
            return response;
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
     * @throws ItemNotFound     if one of the given items does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if data constraints are not met
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/descriptions/{did:[^/]+}/access-points")
    public Response createAccessPoint(@PathParam("id") String id,
                                      @PathParam("did") String did,
                                      Bundle bundle)
            throws PermissionDenied, ValidationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Described item = api().get(id, Described.class);
            Description desc = api().get(did, Description.class);
            AccessPoint rel = api().createDependent(id, bundle,
                    AccessPoint.class, getLogMessage());
            desc.addAccessPoint(rel);
            Response response = buildResponse(item, rel, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Delete the access point with the given ID, on the given description,
     * belonging to the given parent item.
     *
     * @param id   the parent item's ID
     * @param did  the description's ID
     * @param apid the access point's ID
     * @throws ItemNotFound     if one of the given items does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    @DELETE
    @Path("{id:[^/]+}/descriptions/{did:[^/]+}/access-points/{apid:[^/]+}")
    public void deleteAccessPoint(@PathParam("id") String id,
                                  @PathParam("did") String did,
                                  @PathParam("apid") String apid)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            api().deleteDependent(id, apid, getLogMessage());
            tx.success();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Lookup and page the versions for a given item.
     *
     * @param id the event id
     * @return a list of versions
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/versions")
    public Response listVersions(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Versioned item = api().get(id, Versioned.class);
            Response response = streamingPage(() -> getQuery().setStream(true)
                    .page(item.getAllPriorVersions(), Version.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Path("{id:[^/]+}/dc")
    @Produces(MediaType.TEXT_XML)
    public Document exportDc(
            @PathParam("id") String id,
            @QueryParam("lang") String langCode)
            throws ItemNotFound, IOException {
        try (final Tx tx = beginTx()) {
            Described item = api().get(id, Described.class);
            DublinCoreExporter exporter = new DublinCore11Exporter(api());
            Document doc = exporter.export(item, langCode);
            tx.success();
            return doc;
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
        final List<String> ids;
        final List<Long> gids;

        IdSet(List<String> ids, List<Long> gids) {
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
