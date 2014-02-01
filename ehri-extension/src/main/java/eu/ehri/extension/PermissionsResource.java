package eu.ehri.extension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.StreamingOutput;

import eu.ehri.project.acl.GlobalPermissionSet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.AclViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 * <p/>
 * TODO: These functions will typically be called quite frequently for the
 * portal. We should possibly implement some kind of caching system for ACL
 * permissions.
 */
@Path(Entities.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    private final ObjectMapper mapper = new ObjectMapper();

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list/{id:.+}")
    public StreamingOutput listPermissionGrants(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(user.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page/{id:.+}")
    public StreamingOutput pagePermissionGrants(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(SORT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(user.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForItem/{id:.+}")
    public StreamingOutput listPermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/pageForItem/{id:.+}")
    public StreamingOutput pagePermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForScope/{id:.+}")
    public StreamingOutput listPermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/pageForScope/{id:.+}")
    public StreamingOutput pagePermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response getGlobalMatrix() throws PermissionDenied, IOException,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(accessor.getId());
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}")
    public Response getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, IOException, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(acl.getInheritedGlobalPermissions(accessor)))
                .build();
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId
     * @param json
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}")
    public Response setGlobalMatrix(@PathParam("userId") String userId,
            String json) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        HashMap<ContentTypes, List<PermissionType>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        try {
            List<Map<String, GlobalPermissionSet>> newPerms
                    = new AclViews(graph)
                    .setGlobalPermissionMatrix(accessor, globals, grantee);
            graph.getBaseGraph().commit();
            return Response
                    .status(Response.Status.OK)
                    .entity(mapper.writeValueAsBytes(newPerms)).build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Get the permission matrix for a given user on the given entity.
     *
     * @param userId
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/{id:.+}")
    public Response getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, IOException,
            ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = new AclManager(graph, entity.getPermissionScope());

        return Response
                .status(Response.Status.OK)
                .entity(mapper.writeValueAsBytes(acl.getInheritedEntityPermissions(accessor, entity)))
                .build();
    }

    /**
     * Get the user's permissions for a given scope.
     *
     * @param userId
     * @param id
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     * @throws DeserializationError
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public Response getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound,
            IOException, DeserializationError {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = new AclManager(graph, scope);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(acl
                                .getInheritedGlobalPermissions(accessor)))
                .build();
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId the user
     * @param id     the scope id
     * @param json   the serialized permission list
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/scope/{id:.+}")
    public Response setScopedPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id, String json) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();

        try {
            HashMap<ContentTypes, List<PermissionType>> globals = parseMatrix(json);
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph, scope);
            acl.setGlobalPermissionMatrix(accessor, globals, grantee);
            graph.getBaseGraph().commit();
            return getScopedMatrix(userId, id);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Set a user's permissions on a given item.
     *
     * @param id     the item id
     * @param userId the user id
     * @param json   the serialized permission list
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/{id:.+}")
    public Response setItemPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id, String json) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Set<PermissionType> scopedPerms;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<Set<PermissionType>> typeRef = new TypeReference<Set<PermissionType>>() {
            };
            scopedPerms = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }

        Accessor accessor = manager.getFrame(userId, Accessor.class);

        try {
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph);
            acl.setItemPermissions(item, accessor, scopedPerms, grantee);
            graph.getBaseGraph().commit();
            return Response
                    .status(Response.Status.OK)
                    .entity(mapper
                            .writeValueAsBytes(new AclManager(
                                    graph).getInheritedEntityPermissions(accessor,
                                    manager.getFrame(id, AccessibleEntity.class))))
                    .build();
        } finally {
            cleanupTransaction();
        }
    }

    private HashMap<ContentTypes, List<PermissionType>> parseMatrix(String json)
            throws DeserializationError {
        HashMap<ContentTypes, List<PermissionType>> globals;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<HashMap<ContentTypes, List<PermissionType>>> typeRef = new TypeReference<HashMap<ContentTypes,
                    List<PermissionType>>>() {
            };
            globals = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        } catch (IOException e) {
            throw new DeserializationError(e.getMessage());
        }
        return globals;
    }
}