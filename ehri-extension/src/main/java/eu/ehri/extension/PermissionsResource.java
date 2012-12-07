package eu.ehri.extension;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Converter;

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[^/]+}")
    public Response setGlobalMatrix(@PathParam("id") String id, String json)
            throws PermissionDenied, JsonParseException, JsonMappingException,
            IOException, ItemNotFound {

        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String, List<String>>> typeRef = new TypeReference<HashMap<String, List<String>>>() {
        };
        HashMap<String, List<String>> globals = mapper.readValue(json, typeRef);

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            // remove all existing permission grants
            Accessor accessor = manager.getFrame(id, Accessor.class);
            Accessor grantee = getRequesterUserProfile();
            AclManager acl = new AclManager(graph);
            acl.setGlobalPermissionMatrix(accessor, enumifyMatrix(globals));

            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Updated permissions");

            tx.success();
            return getGlobalMatrix(id);
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(Converter.ID_KEY, id);
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
    @Path("/{id:[^/]+}")
    public Response getGlobalMatrix(@PathParam("id") String id)
            throws PermissionDenied, JsonGenerationException,
            JsonMappingException, IOException, ItemNotFound {

        Accessor accessor = manager.getFrame(id, Accessor.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(stringifyInheritedGlobalMatrix(acl
                        .getInheritedGlobalPermissions(accessor)))).build();
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
    @Path("/{userId:[^/]+}/{id:[^/]+}")
    public Response getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied,
            JsonGenerationException, JsonMappingException, IOException,
            ItemNotFound {

        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionGrantTarget entity = manager.getFrame(id,
                PermissionGrantTarget.class);
        AclManager acl = new AclManager(graph);

        return Response
                .status(Status.OK)
                .entity(new ObjectMapper().writeValueAsBytes(stringifyInheritedMatrix(acl
                        .getInheritedEntityPermissions(accessor, entity))))
                .build();
    }

    // Helpers. These just convert from string to internal enum representations
    // of the various permissions-related data structures. 
    // TODO: There's probably a way to get Jackson to do that automatically.
    // This was why scala was invented...

    private List<Map<String,Map<String, List<String>>>> stringifyInheritedGlobalMatrix(
            List<Map<String,Map<ContentTypes, List<PermissionType>>>> matrix) {
        List<Map<String,Map<String, List<String>>>> list = new LinkedList<Map<String,Map<String, List<String>>>>();
        for (Map<String,Map<ContentTypes, List<PermissionType>>> item : matrix) {
            Map<String,Map<String, List<String>>> tmp = new HashMap<String,Map<String, List<String>>>();
            for (Entry<String,Map<ContentTypes, List<PermissionType>>> entry : item.entrySet()) {
                tmp.put(entry.getKey(), stringifyGlobalMatrix(entry.getValue()));
            }
            list.add(tmp);
        }
        return list;
    }
    
    private Map<String,List<String>> stringifyGlobalMatrix(Map<ContentTypes,List<PermissionType>> matrix) {
        Map<String, List<String>> tmp = new HashMap<String, List<String>>();
        for (Entry<ContentTypes,List<PermissionType>> entry : matrix.entrySet()) {
            List<String> ptmp = new LinkedList<String>();
            for (PermissionType pt : entry.getValue()) {
                ptmp.add(pt.getName());
            }
            tmp.put(entry.getKey().getName(), ptmp);
        }
        return tmp;
    }

    private List<Map<String, List<String>>> stringifyInheritedMatrix(
            List<Map<String, List<PermissionType>>> matrix) {
        List<Map<String, List<String>>> tmp = new LinkedList<Map<String, List<String>>>();
        for (Map<String, List<PermissionType>> item : matrix) {
            tmp.add(stringifyMatrix(item));
        }
        return tmp;
    }

    private Map<String, List<String>> stringifyMatrix(
            Map<String, List<PermissionType>> matrix) {
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        for (Entry<String, List<PermissionType>> entry : matrix.entrySet()) {
            List<String> tmp = new LinkedList<String>();
            for (PermissionType t : entry.getValue()) {
                tmp.add(t.getName());
            }
            out.put(entry.getKey(), tmp);
        }
        return out;
    }

    private Map<ContentTypes, List<PermissionType>> enumifyMatrix(
            Map<String, List<String>> matrix) {
        Map<ContentTypes, List<PermissionType>> out = new HashMap<ContentTypes, List<PermissionType>>();
        for (Entry<String, List<String>> entry : matrix.entrySet()) {
            List<PermissionType> tmp = new LinkedList<PermissionType>();
            for (String t : entry.getValue()) {
                tmp.add(PermissionType.withName(t));
            }
            out.put(ContentTypes.withName(entry.getKey()), tmp);
        }
        return out;
    }
}
