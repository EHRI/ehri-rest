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
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path("/access")
public class AccessResource extends EhriNeo4jFramedResource<AccessibleEntity> {

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{atype:[^/]+}/{id:[^/]+}")
    public Response setVisibility(@PathParam("atype") String atype, @PathParam("id") String id,
            String json)
            throws PermissionDenied, JsonParseException, JsonMappingException, IOException {
        
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<LinkedList<Long>> typeRef = new TypeReference<LinkedList<Long>>() {
        };
        LinkedList<Long> accessors = mapper.readValue(json, typeRef);
        
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            AclViews<AccessibleEntity> acl = new AclViews<AccessibleEntity>(graph,
                    cls);
            
            // FIXME: We don't really want to accept accessor IDs here, since
            // the IDs shouldn't leak into the API usage. However, until there
            // is some form of unified global index and id system it's difficult
            // to know how else to do this, since the accessors here could either
            // be users or groups.
            Set<Accessor> accs = new HashSet<Accessor>();
            for (Long aid : accessors) accs.add(graph.getVertex(aid, Accessor.class));

            acl.setAccessors(getEntity(atype, id, AccessibleEntity.class), accs, getRequesterUserProfile());
            tx.success();
            return Response.status(Status.OK).build();
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }        
    }

}
