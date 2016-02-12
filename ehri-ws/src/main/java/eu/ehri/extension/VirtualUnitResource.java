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

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.VirtualUnitViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for the VirtualUnit type
 */
@Path(Entities.VIRTUAL_UNIT)
public final class VirtualUnitResource extends
        AbstractAccessibleResource<VirtualUnit>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String INCLUDED = "includes";

    private final VirtualUnitViews vuViews;

    public VirtualUnitResource(@Context GraphDatabaseService database) {
        super(database, VirtualUnit.class);
        vuViews = new VirtualUnitViews(graph);
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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/list")
    public Response listChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            VirtualUnit parent = manager.getEntity(id, VirtualUnit.class);
            Iterable<VirtualUnit> units = all
                    ? parent.getAllChildren()
                    : parent.getChildren();
            return streamingPage(getQuery(cls).page(units, getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/" + INCLUDED)
    public Response listIncludedVirtualUnits(
            @PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            VirtualUnit parent = manager.getEntity(id, VirtualUnit.class);
            return streamingPage(getQuery(DocumentaryUnit.class)
                    .page(parent.getIncludedUnits(), getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @POST
    @Path("{id:.+}/" + INCLUDED)
    public Response addIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = views.detail(id, currentUser);
            vuViews.addIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser), currentUser);
            tx.success();
            return Response.status(Response.Status.OK).build();
        }
    }

    @DELETE
    @Path("{id:.+}/" + INCLUDED)
    public Response removeIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = views.detail(id, currentUser);
            vuViews.removeIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser), currentUser);
            tx.success();
            return Response.status(Response.Status.OK).build();
        }
    }

    @POST
    @Path("{from:.+}/" + INCLUDED + "/{to:.+}")
    public Response moveIncludedVirtualUnits(
            @PathParam("from") String fromId, @PathParam("to") String toId,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit fromVu = views.detail(fromId, currentUser);
            VirtualUnit toVu = views.detail(toId, currentUser);
            Iterable<DocumentaryUnit> units = getIncludedUnits(includedIds, currentUser);
            vuViews.moveIncludedUnits(fromVu, toVu, units, currentUser);
            tx.success();
            return Response.status(Response.Status.OK).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createTopLevelVirtualUnit(Bundle bundle,
                                              @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                              @QueryParam(ID_PARAM) List<String> includedIds)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            final Accessor currentUser = getCurrentUser();
            final Iterable<DocumentaryUnit> includedUnits
                    = getIncludedUnits(includedIds, currentUser);

            Response item = createItem(bundle, accessors, new Handler<VirtualUnit>() {
                @Override
                public void process(VirtualUnit virtualUnit) {
                    virtualUnit.setAuthor(currentUser);
                    for (DocumentaryUnit include : includedUnits) {
                        virtualUnit.addIncludedUnit(include);
                    }
                }
            });
            tx.success();
            return item;
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/" + Entities.VIRTUAL_UNIT)
    public Response createChildVirtualUnit(@PathParam("id") String id,
                                           Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                           @QueryParam(ID_PARAM) List<String> includedIds)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor currentUser = getRequesterUserProfile();
            final Iterable<DocumentaryUnit> includedUnits
                    = getIncludedUnits(includedIds, currentUser);
            final VirtualUnit parent = views.detail(id, currentUser);

            // NB: Unlike most other items created in another context, virtual
            // units do not inherit the permission scope of their 'parent',
            // because they make have many parents.
            Response item = createItem(bundle, accessors, new Handler<VirtualUnit>() {
                @Override
                public void process(VirtualUnit virtualUnit) {
                    parent.addChild(virtualUnit);
                    for (DocumentaryUnit included : includedUnits) {
                        virtualUnit.addIncludedUnit(included);
                    }
                }
            });
            tx.success();
            return item;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("forUser/{userId:.+}")
    public Response listVirtualUnitsForUser(@PathParam("userId") String userId)
            throws AccessDenied, ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessor currentUser = getRequesterUserProfile();
            Iterable<VirtualUnit> units = vuViews.getVirtualCollectionsForUser(accessor, currentUser);
            return streamingPage(getQuery(cls).page(units, getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Fetch a set of document descriptions from a list of description IDs.
     * We filter these for accessibility and content type (to ensure
     * they actually are the right type.
     */
    private List<DocumentaryUnit> getIncludedUnits(
            List<String> ids, Accessor accessor) throws ItemNotFound {
        Iterable<Vertex> vertices = manager.getVertices(ids);

        PipeFunction<Vertex, Boolean> aclFilter = AclManager.getAclFilterFunction(accessor);

        PipeFunction<Vertex, Boolean> typeFilter = new PipeFunction<Vertex, Boolean>() {
            @Override
            public Boolean compute(Vertex vertex) {
                EntityClass entityClass = manager.getEntityClass(vertex);
                return EntityClass.DOCUMENTARY_UNIT.equals(entityClass);
            }
        };

        GremlinPipeline<Vertex, Vertex> units = new GremlinPipeline<Vertex, Vertex>(
                vertices).filter(typeFilter).filter(aclFilter);
        return Lists.newArrayList(graph.frameVertices(units, DocumentaryUnit.class));
    }
}
