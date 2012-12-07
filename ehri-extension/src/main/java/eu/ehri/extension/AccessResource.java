package eu.ehri.extension;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
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
import org.neo4j.graphdb.Transaction;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.impl.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path("access")
public class AccessResource extends EhriNeo4jFramedResource<AccessibleEntity> {

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id:[^/]+}")
    public Response setVisibility(@PathParam("id") String id, String json)
            throws PermissionDenied, JsonParseException, JsonMappingException,
            IOException {

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            AclViews<AccessibleEntity> acl = new AclViews<AccessibleEntity>(
                    graph, cls);
            acl.setAccessors(manager.getFrame(id, AccessibleEntity.class),
                    extractAccessors(json), getRequesterUserProfile());
            tx.success();
            return Response.status(Status.OK).build();
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
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
     */
    private Set<Accessor> extractAccessors(String json) throws IOException,
            JsonParseException, JsonMappingException, ItemNotFound {
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
    }

}
