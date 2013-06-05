package eu.ehri.extension;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.exceptions.DeserializationError;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Provides a RESTfull interface for generic items.
 */
@Path("entities")
public class GenericResource extends AbstractAccessibleEntityResource<AccessibleEntity> {

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    /**
     * POST alternative to 'get', which allows passing a much larger
     * list of ids to fetch via a JSON body.
     * @param json
     * @return
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     * @throws DeserializationError
     * @throws IOException
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public StreamingOutput getFromJSON(String json)
            throws ItemNotFound, PermissionDenied, BadRequester, DeserializationError, IOException {
        return get(parseIds(json));
    }

    private List<String> parseIds(String json) throws IOException, DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            return mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StreamingOutput get(@QueryParam("id") List<String> ids) throws ItemNotFound,
            PermissionDenied, BadRequester {
        Iterable<Vertex> vertices = manager.getVertices(ids);
        // FIXME: The Acl filter removes items that are not visible to the
        // user only if they are AccessibleEntities. Things like Descriptions
        // pass the filter even if their parent item should not be visible.
        // It's difficult to know how to solve this in a generic and simple
        // manner.
        PipeFunction<Vertex,Boolean> filter = new AclManager(graph)
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                .filter(filter);
        return streamingVertexList(filtered, serializer);
    }
}
