package eu.ehri.extension;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.Query;
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
        AbstractAccessibleEntityResource<VirtualUnit> {

    // Query string key for description IDs
    public static final String DESCRIPTION_ID = "description";

    private final VirtualUnitViews vuViews;
    private final Query<VirtualUnit> virtualUnitQuery;

    public VirtualUnitResource(@Context GraphDatabaseService database) {
        super(database, VirtualUnit.class);
        vuViews = new VirtualUnitViews(graph);
        virtualUnitQuery = new Query<VirtualUnit>(graph, cls);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getVirtualUnit(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listVirtualUnits(
            @QueryParam(PAGE_PARAM) @DefaultValue("1") int page,
            @QueryParam(COUNT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int count,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(page, count, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countVirtualUnits(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public Response listChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(PAGE_PARAM) @DefaultValue("1") int page,
            @QueryParam(COUNT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int count,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        VirtualUnit parent = manager.getFrame(id, VirtualUnit.class);
        Iterable<VirtualUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        Query<VirtualUnit> query = virtualUnitQuery
                .setPage(page).setCount(count).filter(filters)
                .orderBy(order).filter(filters)
                .setStream(isStreaming());
        return streamingPage(query.page(units, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countChildVirtualUnits(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        VirtualUnit parent = manager.getFrame(id, VirtualUnit.class);
        Iterable<VirtualUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        Query<VirtualUnit> query = virtualUnitQuery.filter(filters);
        return numberResponse(query.count(units, getRequesterUserProfile()));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateVirtualUnit(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createTopLevelVirtualUnit(String json,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(DESCRIPTION_ID) List<String> descriptionIds)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor currentUser = getCurrentUser();
        final Iterable<DocumentDescription> documentDescriptions
                = getDocumentDescriptions(descriptionIds, currentUser);

        return create(json, accessors, new Handler<VirtualUnit>() {
            @Override
            public void process(VirtualUnit virtualUnit) {
                virtualUnit.setAuthor(currentUser);
                for (DocumentDescription description : documentDescriptions) {
                    virtualUnit.addReferencedDescription(description);
                }
            }
        });
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateVirtualUnit(@PathParam("id") String id,
            String json) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteVirtualUnit(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return delete(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.VIRTUAL_UNIT)
    public Response createChildVirtualUnit(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            @QueryParam(DESCRIPTION_ID) List<String> descriptionIds)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor currentUser = getRequesterUserProfile();
        final Iterable<DocumentDescription> documentDescriptions
                = getDocumentDescriptions(descriptionIds, currentUser);
        final VirtualUnit parent = views.detail(id, currentUser);
        return create(json, accessors, new Handler<VirtualUnit>() {
            @Override
            public void process(VirtualUnit virtualUnit) {
                parent.addChild(virtualUnit);
                for (DocumentDescription description : documentDescriptions) {
                    virtualUnit.addReferencedDescription(description);
                }
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/forUser/{userId:.+}")
    public Response listVirtualUnitsForUser(@PathParam("userId") String userId,
            @QueryParam(PAGE_PARAM) @DefaultValue("1") int page,
            @QueryParam(COUNT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int count,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws AccessDenied, ItemNotFound, BadRequester {
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor currentUser = getRequesterUserProfile();
        Iterable<VirtualUnit> units = vuViews.getVirtualCollectionsForUser(accessor, currentUser);
        Query.Page<VirtualUnit> list = virtualUnitQuery
                .filter(filters).setPage(page).setCount(count).orderBy(order)
                .setStream(isStreaming())
                .page(units, currentUser);
        return streamingPage(list);
    }

    /**
     * Fetch a set of document descriptions from a list of description IDs.
     * We filter these for accessibility and content type (to ensure
     * they actually are the right type.
     */
    private Iterable<DocumentDescription> getDocumentDescriptions(
            List<String> descriptionIds, Accessor accessor)
            throws ItemNotFound, BadRequester {
        Iterable<Vertex> vertices = manager.getVertices(descriptionIds);

        PipeFunction<Vertex, Boolean> aclFilter = aclManager.getAclFilterFunction(accessor);

        PipeFunction<Vertex, Boolean> typeFilter = new PipeFunction<Vertex, Boolean>() {
            @Override
            public Boolean compute(Vertex vertex) {
                EntityClass entityClass = manager.getEntityClass(vertex);
                return entityClass != null && entityClass
                        .equals(EntityClass.DOCUMENT_DESCRIPTION);
            }
        };

        GremlinPipeline<Vertex, Vertex> descriptions = new GremlinPipeline<Vertex, Vertex>(
                vertices).filter(typeFilter).filter(aclFilter);

        return graph.frameVertices(descriptions.toList(), DocumentDescription.class);
    }
}
