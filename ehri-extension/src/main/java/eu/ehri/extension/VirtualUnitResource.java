package eu.ehri.extension;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.extension.errors.BadRequester;
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
 * Provides a Restful interface for the VirtualUnit type
 */
@Path(Entities.VIRTUAL_UNIT)
public final class VirtualUnitResource extends
        AbstractAccessibleEntityResource<VirtualUnit>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String INCLUDED = "includes";

    private final VirtualUnitViews vuViews;

    public VirtualUnitResource(@Context GraphDatabaseService database) {
        super(database, VirtualUnit.class);
        vuViews = new VirtualUnitViews(graph);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public Response listChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        VirtualUnit parent = manager.getFrame(id, VirtualUnit.class);
        Iterable<VirtualUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        return streamingPage(getQuery(cls).page(units, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + INCLUDED)
    public Response listIncludedVirtualUnits(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, PermissionDenied {
        VirtualUnit parent = manager.getFrame(id, VirtualUnit.class);
        return streamingPage(getQuery(DocumentaryUnit.class)
                .page(parent.getIncludedUnits(), getRequesterUserProfile()));
    }

    @POST
    @Path("/{id:.+}/" + INCLUDED)
    public Response addIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, BadRequester, PermissionDenied {
        try {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = views.detail(id, currentUser);
            vuViews.addIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser), currentUser);
            graph.getBaseGraph().commit();
            return Response.status(Response.Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("/{id:.+}/" + INCLUDED)
    public Response removeIncludedVirtualUnits(
            @PathParam("id") String id, @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, BadRequester, PermissionDenied {
        try {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit parent = views.detail(id, currentUser);
            vuViews.removeIncludedUnits(parent,
                    getIncludedUnits(includedIds, currentUser), currentUser);
            graph.getBaseGraph().commit();
            return Response.status(Response.Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }

    @POST
    @Path("/{from:.+}/" + INCLUDED + "/{to:.+}")
    public Response moveIncludedVirtualUnits(
            @PathParam("from") String fromId, @PathParam("to") String toId,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws ItemNotFound, BadRequester, PermissionDenied {
        try {
            UserProfile currentUser = getCurrentUser();
            VirtualUnit fromVu = views.detail(fromId, currentUser);
            VirtualUnit toVu = views.detail(toId, currentUser);
            Iterable<DocumentaryUnit> units = getIncludedUnits(includedIds, currentUser);
            vuViews.removeIncludedUnits(fromVu, units, currentUser);
            vuViews.addIncludedUnits(toVu, units, currentUser);
            graph.getBaseGraph().commit();
            return Response.status(Response.Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public long countChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        VirtualUnit parent = manager.getFrame(id, VirtualUnit.class);
        Iterable<VirtualUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        return getQuery(cls).count(units);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createTopLevelVirtualUnit(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor currentUser = getCurrentUser();
        final Iterable<DocumentaryUnit> includedUnits
                = getIncludedUnits(includedIds, currentUser);

        return createItem(bundle, accessors, new Handler<VirtualUnit>() {
            @Override
            public void process(VirtualUnit virtualUnit) {
                virtualUnit.setAuthor(currentUser);
                for (DocumentaryUnit include : includedUnits) {
                    virtualUnit.addIncludedUnit(include);
                }
            }
        });
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response update(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return updateItem(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id,
                           Bundle bundle) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return updateItem(id, bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return deleteItem(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.VIRTUAL_UNIT)
    public Response createChildVirtualUnit(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(ID_PARAM) List<String> includedIds)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor currentUser = getRequesterUserProfile();
        final Iterable<DocumentaryUnit> includedUnits
                = getIncludedUnits(includedIds, currentUser);
        final VirtualUnit parent = views.detail(id, currentUser);

        // NB: Unlike most other items created in another context, virtual
        // units do not inherit the permission scope of their 'parent',
        // because they make have many parents.
        return createItem(bundle, accessors, new Handler<VirtualUnit>() {
            @Override
            public void process(VirtualUnit virtualUnit) {
                parent.addChild(virtualUnit);
                for (DocumentaryUnit included : includedUnits) {
                    virtualUnit.addIncludedUnit(included);
                }
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/forUser/{userId:.+}")
    public Response listVirtualUnitsForUser(@PathParam("userId") String userId)
            throws AccessDenied, ItemNotFound, BadRequester {
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor currentUser = getRequesterUserProfile();
        Iterable<VirtualUnit> units = vuViews.getVirtualCollectionsForUser(accessor, currentUser);
        return streamingPage(getQuery(cls).page(units, getRequesterUserProfile()));
    }

    /**
     * Fetch a set of document descriptions from a list of description IDs.
     * We filter these for accessibility and content type (to ensure
     * they actually are the right type.
     */
    private Iterable<DocumentaryUnit> getIncludedUnits(
            List<String> ids, Accessor accessor)
            throws ItemNotFound, BadRequester {
        Iterable<Vertex> vertices = manager.getVertices(ids);

        PipeFunction<Vertex, Boolean> aclFilter = aclManager.getAclFilterFunction(accessor);

        PipeFunction<Vertex, Boolean> typeFilter = new PipeFunction<Vertex, Boolean>() {
            @Override
            public Boolean compute(Vertex vertex) {
                EntityClass entityClass = manager.getEntityClass(vertex);
                return entityClass != null && entityClass
                        .equals(EntityClass.DOCUMENTARY_UNIT);
            }
        };

        GremlinPipeline<Vertex, Vertex> units = new GremlinPipeline<Vertex, Vertex>(
                vertices).filter(typeFilter).filter(aclFilter);

        return graph.frameVertices(units, DocumentaryUnit.class);
    }
}
