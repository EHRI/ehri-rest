package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.*;
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
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Provides a RESTful(ish) interface for setting PermissionTarget perms.
 *
 * @author http://github.com/mikesname
 */
@Path(Entities.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    private final ObjectMapper mapper = new ObjectMapper();

    private final AclManager aclManager;
    private final AclViews aclViews;

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
        aclManager = new AclManager(graph);
        aclViews = new AclViews(graph);
    }

    private CacheControl getCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(AbstractRestResource.ITEM_CACHE_TIME);
        return cacheControl;
    }

    /**
     * Get a list of permission grants for the given user
     *
     * @param id The user's id
     * @return A list of permission grants for the user
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list/{id:.+}")
    public Response listPermissionGrants(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(user.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return A list of grants for this item
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForItem/{id:.+}")
    public Response listPermissionGrantsForItem(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(target.getPermissionGrants(), accessor, PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return A list of grants for the given scope
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForScope/{id:.+}")
    public Response listPermissionGrantsForScope(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return The current user's global permissions
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalMatrix() throws PermissionDenied, IOException,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(accessor.getId());
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId The user ID
     * @return The user's global permissions
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
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(aclManager
                                .getInheritedGlobalPermissions(accessor)))
                .cacheControl(getCacheControl())
                .build();
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId The user ID
     * @param json   The permission matrix data
     * @return The new permissions
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
        graph.getBaseGraph().checkNotInTransaction();
        GlobalPermissionSet globals = parseMatrix(json);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        try {
            InheritedGlobalPermissionSet newPerms
                    = aclViews
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
     * @param userId The user's ID
     * @param id     The item id
     * @return The user's permissions for that item
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
                                    @PathParam("id") String id) throws PermissionDenied, IOException,
            ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = aclManager.withScope(entity.getPermissionScope());

        return Response
                .status(Response.Status.OK)
                .entity(mapper.writeValueAsBytes(acl.getInheritedItemPermissions(entity, accessor)))
                .cacheControl(getCacheControl())
                .build();
    }

    /**
     * Get the user's permissions for a given scope.
     *
     * @param userId The user's permissions
     * @param id     The scope ID
     * @return The matrix for the given scope
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
            IOException, DeserializationError {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = aclManager.withScope(scope);

        return Response
                .status(Response.Status.OK)
                .entity(mapper
                        .writeValueAsBytes(acl
                                .getInheritedGlobalPermissions(accessor)))
                .cacheControl(getCacheControl())
                .build();
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId the user
     * @param id     the scope id
     * @param json   the serialized permission list
     * @return The new permission matrix
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
            GlobalPermissionSet globals = parseMatrix(json);
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = aclViews.withScope(scope);
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
    @Produces(MediaType.APPLICATION_JSON)
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

        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);

            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph);
            acl.setItemPermissions(item, accessor, scopedPerms, grantee);
            graph.getBaseGraph().commit();
            return Response
                    .status(Response.Status.OK)
                    .entity(mapper
                            .writeValueAsBytes(new AclManager(
                                    graph).getInheritedItemPermissions(manager.getFrame(id, AccessibleEntity.class), accessor
                            )))
                    .build();
        } finally {
            cleanupTransaction();
        }
    }

    private GlobalPermissionSet parseMatrix(String json) throws DeserializationError {
        HashMap<ContentTypes, Collection<PermissionType>> globals;
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
        return GlobalPermissionSet.from(globals);
    }
}