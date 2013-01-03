package eu.ehri.extension;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.impl.AclViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 * 
 * TODO: These functions will typically be called quite frequently for the
 * portal. We should possibly implement some kind of caching system for ACL
 * permissions.
 * 
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
    @Path("/list/{id:.+}")
    public StreamingOutput listPermissionGrants(
            @PathParam("id") String id,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws PermissionDenied, ItemNotFound, BadRequester {
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class);
        return streamingPage(query.page(user.getPermissionGrants(), accessor,
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
            JsonGenerationException, JsonMappingException, IOException,
            ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(manager.getId(accessor));
    }

    /**
     * Get the global permission matrix for the given accessor.
     * 
     * @param id
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
            throws PermissionDenied, JsonGenerationException,
            JsonMappingException, IOException, ItemNotFound {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper()
                        .writeValueAsBytes(stringifyInheritedGlobalMatrix(acl
                                .getInheritedGlobalPermissions(accessor))))
                .build();
    }

    /**
     * Set a user's global permission matrix.
     * 
     * @param id
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public Response setGlobalMatrix(@PathParam("userId") String userId, String json)
            throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        HashMap<String, List<String>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        new AclViews(graph).setGlobalPermissionMatrix(accessor,
                enumifyMatrix(globals), grantee);
        return getGlobalMatrix(userId);
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public Response getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied,
            JsonGenerationException, JsonMappingException, IOException,
            ItemNotFound {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(stringifyInheritedMatrix(acl
                        .getInheritedEntityPermissions(accessor, entity))))
                .build();
    }

    /**
     * Get the user's permissions for a given scope.
     * 
     * @param userId
     * @param scopeId
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
    public Response getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound,
            JsonGenerationException, JsonMappingException, IOException,
            DeserializationError {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(stringifyInheritedGlobalMatrix(acl
                        .getInheritedScopedPermissions(accessor, scope))))
                .build();
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     * 
     * @param id
     *            the scope id
     * @param type
     *            the content type
     * @param user
     *            the user id
     * @param json
     *            the serialized permission list
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
    public Response setScopedPermissions(
            @PathParam("userId") String userId,
            @PathParam("id") String id,
            String json)
            throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        HashMap<String, List<String>> globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor grantee = getRequesterUserProfile();
        AclViews acl = new AclViews(graph, scope);

        acl.setScopedPermissions(accessor, enumifyMatrix(globals), grantee);
        return getScopedMatrix(userId, id);
    }

    /**
     * Set a user's permissions on a given item.
     * 
     * @param id
     *            the item id
     * @param userId
     *            the user id
     * @param json
     *            the serialized permission list
     * @return
     * 
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/{userId:.+}")
    public Response setItemPermissions(@PathParam("id") String id,
            @PathParam("userId") String userId, String json)
            throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        List<String> scopedPerms;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            scopedPerms = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }

        AccessibleEntity item = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        AclViews acl = new AclViews(graph, item.getScope());

        acl.setItemPermissions(item, accessor,
                enumifyPermissionList(scopedPerms), grantee);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper()
                        .writeValueAsBytes(stringifyInheritedMatrix(new AclManager(
                                graph).getInheritedEntityPermissions(accessor,
                                manager.getFrame(id, AccessibleEntity.class)))))
                .build();
    }

    // Helpers. These just convert from string to internal enum representations
    // of the various permissions-related data structures.
    // TODO: There's probably a way to get Jackson to do that automatically.
    // This was why scala was invented...

    private HashMap<String, List<String>> parseMatrix(String json)
            throws IOException, JsonParseException, DeserializationError {
        HashMap<String, List<String>> globals;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<HashMap<String, List<String>>> typeRef = new TypeReference<HashMap<String, List<String>>>() {
            };
            globals = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
        return globals;
    }

    private List<Map<String, Map<String, List<String>>>> stringifyInheritedGlobalMatrix(
            List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> list2) {
        List<Map<String, Map<String, List<String>>>> list = Lists
                .newLinkedList();
        for (Map<String, Map<ContentTypes, Collection<PermissionType>>> item : list2) {
            Map<String, Map<String, List<String>>> tmp = Maps.newHashMap();
            for (Entry<String, Map<ContentTypes, Collection<PermissionType>>> entry : item
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
        for (Entry<ContentTypes, Collection<PermissionType>> entry : map
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
        for (Entry<String, List<PermissionType>> entry : matrix.entrySet()) {
            List<String> tmp = Lists.newLinkedList();
            for (PermissionType t : entry.getValue()) {
                tmp.add(t.getName());
            }
            out.put(entry.getKey(), tmp);
        }
        return out;
    }

    /**
     * Convert a permission matrix containing strings in lieu of content type
     * and permission type enum values to the enum version. If Jackson 1.9 were
     * available in Neo4j we wouldn't need this, since its @JsonCreator
     * annotation allows specifying how to deserialize those enums properly.
     * 
     * @param matrix
     * @return
     * @throws DeserializationError
     */
    private Map<ContentTypes, List<PermissionType>> enumifyMatrix(
            Map<String, List<String>> matrix) throws DeserializationError {
        try {
            Map<ContentTypes, List<PermissionType>> out = Maps.newHashMap();
            for (Entry<String, List<String>> entry : matrix.entrySet()) {
                List<PermissionType> tmp = Lists.newLinkedList();
                for (String t : entry.getValue()) {
                    tmp.add(PermissionType.withName(t));
                }
                out.put(ContentTypes.withName(entry.getKey()), tmp);
            }
            return out;
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    private Set<PermissionType> enumifyPermissionList(List<String> scopedPerms)
            throws DeserializationError {
        try {
            Set<PermissionType> perms = Sets.newHashSet();
            for (String p : scopedPerms) {
                perms.add(PermissionType.withName(p));
            }

            return perms;
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}
