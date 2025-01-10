/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.ws;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
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
import eu.ehri.project.ws.base.*;
import org.neo4j.dbms.api.DatabaseManagementService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for the VirtualUnit type
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.VIRTUAL_UNIT)
public final class VirtualUnitResource extends
        AbstractAccessibleResource<VirtualUnit>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public VirtualUnitResource(@Context DatabaseManagementService service) {
        super(service, VirtualUnit.class);
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/list")
    public Response listChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response page = streamingPage(() -> {
                VirtualUnit parent = manager.getEntityUnchecked(id, cls);
                Iterable<VirtualUnit> units = all
                        ? parent.getAllChildren()
                        : parent.getChildren();
                return getQuery().page(units, cls);
            });
            tx.success();
            return page;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/includes")
    public Response listIncludedVirtualUnits(
            @PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response page = streamingPage(() -> {
                VirtualUnit parent = manager.getEntityUnchecked(id, cls);
                return getQuery()
                        .page(parent.getIncludedUnits(), DocumentaryUnit.class);
            });
            tx.success();
            return page;
        }
    }

    @POST
    @Path("{id:[^/]+}/includes")
    public Response addIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = api().get(id, cls);
            Response item = single(api().virtualUnits().addIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser)));
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}/includes")
    public Response removeIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = api().get(id, cls);
            Response item = single(api().virtualUnits().removeIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser)));
            tx.success();
            return item;
        }
    }

    @POST
    @Path("{from:[^/]+}/includes/{to:[^/]+}")
    public void moveIncludedVirtualUnits(
            @PathParam("from") String fromId, @PathParam("to") String toId,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, PermissionDenied {
        try (final Tx tx = beginTx()) {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit fromVu = api().get(fromId, cls);
            VirtualUnit toVu = api().get(toId, cls);
            Iterable<DocumentaryUnit> units = getIncludedUnits(includedIds, currentUser);
            api().virtualUnits().moveIncludedUnits(fromVu, toVu, units);
            tx.success();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTopLevelVirtualUnit(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws PermissionDenied, ValidationError,
            DeserializationError {
        try (final Tx tx = beginTx()) {
            final Accessor currentUser = getCurrentUser();
            final Iterable<DocumentaryUnit> includedUnits
                    = getIncludedUnits(includedIds, currentUser);

            Response item = createItem(bundle, accessors, virtualUnit -> {
                virtualUnit.setAuthor(currentUser);
                for (DocumentaryUnit include : includedUnits) {
                    virtualUnit.addIncludedUnit(include);
                }
            });
            tx.success();
            return item;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    public Response createChildVirtualUnit(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor currentUser = getRequesterUserProfile();
            final Iterable<DocumentaryUnit> includedUnits
                    = getIncludedUnits(includedIds, currentUser);
            final VirtualUnit parent = api().get(id, cls);

            // NB: Unlike most other items created in another context, virtual
            // units do not inherit the permission scope of their 'parent',
            // because they make have many parents.
            Response item = createItem(bundle, accessors, virtualUnit -> {
                parent.addChild(virtualUnit);
                for (DocumentaryUnit included : includedUnits) {
                    virtualUnit.addIncludedUnit(included);
                }
            });
            tx.success();
            return item;
        }
    }

    /**
     * Fetch a set of document descriptions from a list of description IDs.
     * We filter these for accessibility and content type (to ensure
     * they actually are the right type.
     */
    private List<DocumentaryUnit> getIncludedUnits(
            List<String> ids, Accessor accessor) {
        Iterable<Vertex> vertices = manager.getVertices(ids);

        PipeFunction<Vertex, Boolean> aclFilter = AclManager.getAclFilterFunction(accessor);

        PipeFunction<Vertex, Boolean> typeFilter = vertex -> {
            EntityClass entityClass = manager.getEntityClass(vertex);
            return EntityClass.DOCUMENTARY_UNIT.equals(entityClass);
        };

        GremlinPipeline<Vertex, Vertex> units = new GremlinPipeline<Vertex, Vertex>(
                vertices).filter(typeFilter).filter(aclFilter);
        return Lists.newArrayList(graph.frameVertices(units, DocumentaryUnit.class));
    }
}
