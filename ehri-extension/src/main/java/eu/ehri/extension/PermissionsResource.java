package eu.ehri.extension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistance.ActionManager;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 * 
 * TODO: These functions will typically be called quite frequently for the
 * portal. We should possibly implement some kind of caching system for ACL
 * permissions.
 * 
 */
@Path(EntityTypes.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{atype:[^/]+}/{id:[^/]+}")
    public Response setGlobalMatrix(@PathParam("atype") String atype,
            @PathParam("id") String id, String json) throws PermissionDenied,
            JsonParseException, JsonMappingException, IOException, ItemNotFound {

        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String, List<String>>> typeRef = new TypeReference<HashMap<String, List<String>>>() {
        };
        HashMap<String, List<String>> globals = mapper.readValue(json, typeRef);

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            // remove all existing permission grants
            Accessor accessor = getEntity(atype, id, Accessor.class);
            Accessor grantee = getRequesterUserProfile();
            AclManager acl = new AclManager(graph);
            acl.setGlobalPermissionMatrix(accessor, globals);

            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Updated permissions");

            tx.success();
            return getGlobalMatrix(atype, id);
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Get the global permission matrix for the given accessor.
     * 
     * @param atype
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
    @Path("/{atype:[^/]+}/{id:[^/]+}")
    public Response getGlobalMatrix(@PathParam("atype") String atype,
            @PathParam("id") String id) throws PermissionDenied,
            JsonGenerationException, JsonMappingException, IOException,
            ItemNotFound {

        Accessor accessor = getEntity(atype, id, Accessor.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(acl
                        .getInheritedGlobalPermissions(accessor))).build();
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
        return getGlobalMatrix(EntityTypes.USER_PROFILE,
                accessor.getIdentifier());
    }

    /**
     * Get the permission matrix for a given user on the given entity.
     * 
     * @param atype
     * @param userId
     * @param ctype
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
    @Path("/{atype:[^/]+}/{userId:[^/]+}/{ctype:[^/]+}/{id:[^/]+}")
    public Response getEntityMatrix(@PathParam("atype") String atype,
            @PathParam("userId") String userId,
            @PathParam("ctype") String ctype, @PathParam("id") String id)
            throws PermissionDenied, JsonGenerationException,
            JsonMappingException, IOException, ItemNotFound {

        Accessor accessor = getEntity(atype, userId, Accessor.class);
        PermissionGrantTarget entity = getEntity(ctype, id,
                PermissionGrantTarget.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(acl
                        .getInheritedEntityPermissions(accessor, entity)))
                .build();
    }
}
