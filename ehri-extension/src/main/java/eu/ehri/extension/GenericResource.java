package eu.ehri.extension;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Provides a means of fetching items and lists of items
 * regardless of their specific type.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("entities")
public class GenericResource extends AbstractAccessibleEntityResource<AccessibleEntity> {

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param ids A list of string IDs
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response list(@QueryParam("id") List<String> ids) throws ItemNotFound,
            PermissionDenied, BadRequester {
        // Object a lazily-computed view of the ids->vertices...
        Iterable<Vertex> vertices = manager.getVertices(ids);
        PipeFunction<Vertex, Boolean> filter = aclManager
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
        return streamingVertexList(filtered, getSerializer());
    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param json A JSON-encoded list of string IDs
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listFromJson(String json)
            throws ItemNotFound, PermissionDenied, BadRequester, DeserializationError, IOException {
        return list(parseIds(json));
    }

    /**
     * Fetch a list of items by their internal graph ID (gid), which is
     * provided in the bundle metadata section of the default response.
     *
     * @param ids A list of graph IDs
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public Response listByGid(@QueryParam("gid") List<Long> ids) throws ItemNotFound,
            PermissionDenied, BadRequester {
        // This is ugly, but to return 404 on a bad item we have to
        // iterate the list first otherwise the streaming response will be
        // broken.
        for (Long id : ids) {
            if (graph.getVertex(id) == null) {
                throw new ItemNotFound(String.valueOf(id));
            }
        }

        FluentIterable<Vertex> vertices = FluentIterable.from(ids)
                .transform(new Function<Long, Vertex>() {
                    public Vertex apply(Long id) {
                        return graph.getVertex(id);
                    }
                });

        PipeFunction<Vertex, Boolean> filter = aclManager
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
        return streamingVertexList(filtered, getSerializer());
    }

    /**
     * Fetch a list of items by their internal graph ID (gid), which is
     * provided in the bundle metadata section of the default response.
     *
     * @param json A JSON-encoded list of graph IDs
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public Response listByGidFromJson(String json) throws ItemNotFound,
            PermissionDenied, DeserializationError, BadRequester, IOException {

        return listByGid(parseGraphIds(json));
    }

    /**
     * Fetch an item of any type by ID.
     *
     * @param id The item's ID
     * @return A serialized representation.
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/{id:.+}")
    public Response get(@PathParam("id") String id) throws ItemNotFound, AccessDenied, BadRequester {
        Vertex item = manager.getVertex(id);

        // If the item doesn't exist or isn't a content type throw 404
        Accessor currentUser = getRequesterUserProfile();
        if (item == null || !aclManager.getContentTypeFilterFunction().compute(item)) {
            throw new ItemNotFound(id);
        } else if (!aclManager.getAclFilterFunction(currentUser).compute(item)) {
            throw new AccessDenied(currentUser.getId(), id);
        }

        return single(item);
    }

    private List<Long> parseGraphIds(String json) throws IOException, DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<Long>> typeRef = new TypeReference<List<Long>>() {
            };
            return mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
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
}
