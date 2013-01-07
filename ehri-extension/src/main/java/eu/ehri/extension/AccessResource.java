package eu.ehri.extension;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.impl.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path("access")
public class AccessResource extends
        AbstractAccessibleEntityResource<AccessibleEntity> {

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    /**
     * Set the accessors who are able to view an item. If no accessors
     * are set, the item is globally readable.
     *  
     * @param id
     * @param json
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws DeserializationError
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id:[^/]+}")
    public Response setVisibility(@PathParam("id") String id, String json)
            throws PermissionDenied, ItemNotFound, BadRequester,
            DeserializationError, IOException {
        Set<Accessor> accessors = extractAccessors(json);
        AclViews acl = new AclViews(graph);
        acl.setAccessors(manager.getFrame(id, AccessibleEntity.class),
                accessors, getRequesterUserProfile());
        return Response.status(Status.OK).build();
    }

    /**
     * Parse the incoming JSON describing which accessors can view the item. It
     * should be in the format: { "userProfile": ["mike", "repo"], "group":
     * ["kcl", "niod"] }
     * 
     * @param json
     * @return
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws ItemNotFound
     * @throws DeserializationError
     */
    private Set<Accessor> extractAccessors(String json) throws ItemNotFound,
            DeserializationError, JsonMappingException, IOException {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<LinkedList<String>> typeRef = new TypeReference<LinkedList<String>>() {
            };
            LinkedList<String> accessorList = mapper.readValue(json, typeRef);

            Set<Accessor> accs = new HashSet<Accessor>();
            for (String at : accessorList) {
                accs.add(manager.getFrame(at, Accessor.class));
            }
            return accs;
        } catch (JsonParseException e) {
            throw new DeserializationError("Unable to parse accessor list", e);
        }
    }
}
