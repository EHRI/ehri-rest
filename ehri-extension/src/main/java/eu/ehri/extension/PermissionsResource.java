package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

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
import eu.ehri.project.views.impl.AclViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 * <p/>
 * TODO: These functions will typically be called quite frequently for the
 * portal. We should possibly implement some kind of caching system for ACL
 * permissions.
 */
@Path(Entities.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @param id
     * @param offset
     * @param limit
     * @param order
     * @param filters
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
     * @param id
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/page/{id:.+}")
    public StreamingOutput pagePermissionGrants(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(SORT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
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
     * @param id
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForItem/{id:.+}")
    public StreamingOutput listPermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
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
     * @param id
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pageForItem/{id:.+}")
    public StreamingOutput pagePermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
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
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForScope/{id:.+}")
    public StreamingOutput listPermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
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
     * @param id
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pageForScope/{id:.+}")
    public StreamingOutput pagePermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalMatrix() throws PermissionDenied,
            IOException, ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(manager.getId(accessor));
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId the user id
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public Response getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, IOException, ItemNotFound {
        System.out.println("RETURNING MATRIX FOR: " + userId);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AclManager acl = new AclManager(graph);

        final List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> perms = acl
                .getInheritedGlobalPermissions(accessor);

        final ObjectMapper mapper = new ObjectMapper();
        return Response.status(Response.Status.OK).entity(mapper.writeValueAsBytes(stringifyInheritedGlobalMatrix(perms))).build();
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId the user id
     * @param json   the global permission matrix
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public Response setGlobalMatrix(@PathParam("userId") String userId,
            String json) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        Map<ContentTypes, List<PermissionType>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        new AclViews(graph).setGlobalPermissionMatrix(accessor, globals,
                grantee);
        return getGlobalMatrix(userId);
    }

    /**
     * Get the permission matrix for a given user on the given entity.
     *
     * @param userId the user id
     * @param id     the item id
     * @return
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public StreamingOutput getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, IOException,
            ItemNotFound {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = new AclManager(graph, entity.getPermissionScope());

        final List<Map<String, List<PermissionType>>> perms = acl
                .getInheritedEntityPermissions(accessor, entity);
        final ObjectMapper mapper = new ObjectMapper();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                mapper.writeValue(output, perms);
            }
        };
    }

    /**
     * Get the user's permissions for a given scope.
     *
     * @param userId the user id
     * @param id     the scope id
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     * @throws DeserializationError
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public StreamingOutput getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound,
            IOException, DeserializationError {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = new AclManager(graph, scope);

        final List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> perms = acl
                .getInheritedGlobalPermissions(accessor);
        final ObjectMapper mapper = new ObjectMapper();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                mapper.writeValue(output, perms);
            }
        };
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId the user id
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public StreamingOutput setScopedPermissions(
            @PathParam("userId") String userId, @PathParam("id") String id,
            String json) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        Map<ContentTypes, List<PermissionType>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor grantee = getRequesterUserProfile();
        AclViews acl = new AclViews(graph, scope);

        acl.setGlobalPermissionMatrix(accessor, globals, grantee);
        return getScopedMatrix(userId, id);
    }

    /**
     * Set a user's permissions on a given item.
     *
     * @param id     the item id
     * @param userId the user id
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public StreamingOutput setItemPermissions(
            @PathParam("userId") String userId, @PathParam("id") String id,
            String json) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        Set<PermissionType> scopedPerms = parseList(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity item = manager.getFrame(id, PermissionScope.class);
        Accessor grantee = getRequesterUserProfile();
        AclViews acl = new AclViews(graph);
        acl.setItemPermissions(item, accessor, scopedPerms, grantee);

        final List<Map<String, List<PermissionType>>> perms = new AclManager(
                graph).getInheritedEntityPermissions(accessor,
                manager.getFrame(id, AccessibleEntity.class));
        final ObjectMapper mapper = new ObjectMapper();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                mapper.writeValue(output, perms);
            }
        };
    }

    // Helpers. These just convert from string to internal enum representations
    // of the various permissions-related data structures.

    private Set<PermissionType> parseList(String json) throws IOException,
            DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<PermissionType>> typeRef = new TypeReference<List<PermissionType>>() {
            };
            List<PermissionType> value = mapper.readValue(json, typeRef);
            return Sets.immutableEnumSet(value);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    private Map<ContentTypes, List<PermissionType>> parseMatrix(String json)
            throws IOException, DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<HashMap<ContentTypes, List<PermissionType>>> typeRef = new TypeReference<HashMap<ContentTypes, List<PermissionType>>>() {
            };
            HashMap<ContentTypes, List<PermissionType>> value = mapper
                    .readValue(json, typeRef);
            return ImmutableMap.copyOf(value);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    // Helpers. These just convert from string to internal enum representations
    // of the various permissions-related data structures.
    // TODO: There's probably a way to get Jackson to do that automatically.
    // This was why scala was invented...

    private List<Map<String, Map<String, List<String>>>> stringifyInheritedGlobalMatrix(
            List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> list2) {
        List<Map<String, Map<String, List<String>>>> list = Lists
                .newLinkedList();
        for (Map<String, Map<ContentTypes, Collection<PermissionType>>> item : list2) {
            Map<String, Map<String, List<String>>> tmp = Maps.newHashMap();
            for (Map.Entry<String, Map<ContentTypes, Collection<PermissionType>>> entry : item
                    .entrySet()) {
                tmp.put(entry.getKey(), stringifyGlobalMatrix(entry.getValue()));
            }
            list.add(tmp);
        }
        return list;
    }

    private Map<String, List<String>> stringifyGlobalMatrix(
            Map<ContentTypes, Collection<PermissionType>> map) {
        Map<String, List<String>> tmp = Maps.newHashMap();
        for (Map.Entry<ContentTypes, Collection<PermissionType>> entry : map
                .entrySet()) {
            List<String> ptmp = Lists.newLinkedList();
            for (PermissionType pt : entry.getValue()) {
                ptmp.add(pt.getName());
            }
            tmp.put(entry.getKey().getName(), ptmp);
        }
        return tmp;
    }

    private List<Map<String, List<String>>> stringifyInheritedMatrix(
            List<Map<String, List<PermissionType>>> matrix) {
        List<Map<String, List<String>>> tmp = Lists.newLinkedList();
        for (Map<String, List<PermissionType>> item : matrix) {
            tmp.add(stringifyMatrix(item));
        }
        return tmp;
    }

    private Map<String, List<String>> stringifyMatrix(
            Map<String, List<PermissionType>> matrix) {
        Map<String, List<String>> out = Maps.newHashMap();
        for (Map.Entry<String, List<PermissionType>> entry : matrix.entrySet()) {
            List<String> tmp = Lists.newLinkedList();
            for (PermissionType t : entry.getValue()) {
                tmp.add(t.getName());
            }
            out.put(entry.getKey(), tmp);
        }
        return out;
    }
}
