package eu.ehri.extension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

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

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.views.AclViews;

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
            Accessor grantee = getEntity(EntityTypes.USER_PROFILE,
                    getRequesterIdentifier(), Accessor.class);
            AclViews<Group> acl = new AclViews<Group>(graph, Group.class);
            acl.setGlobalPermissionMatrix(accessor, grantee, globals);

            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Updated permissions");

            tx.success();
            return getGlobalMatrix(atype, id);
        } catch (PermissionDenied e) {
            tx.failure();
            throw e;
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
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalMatrix() throws PermissionDenied,
            JsonGenerationException, JsonMappingException, IOException,
            ItemNotFound {
        String reqId = getRequesterIdentifier();
        if (reqId == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        return getGlobalMatrix(EntityTypes.USER_PROFILE, reqId);
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

    // FIXME: Copied and pasted this from the AbstractViews class.
    // Need to work out a better place to put it.
    private <E> E getEntity(String typeName, String name, Class<E> cls)
            throws ItemNotFound {
        // FIXME: Ensure index isn't null
        Index<Vertex> index = graph.getBaseGraph().getIndex(typeName,
                Vertex.class);

        CloseableIterable<Vertex> query = index.get(
                AccessibleEntity.IDENTIFIER_KEY, name);
        try {
            return graph.frame(query.iterator().next(), cls);
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(AccessibleEntity.IDENTIFIER_KEY, name);
        } finally {
            query.close();
        }
    }
}
