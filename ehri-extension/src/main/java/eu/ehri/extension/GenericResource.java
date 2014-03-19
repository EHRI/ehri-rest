package eu.ehri.extension;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
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

    final AclManager aclManager;

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
        aclManager = new AclManager(graph);
    }

    /**
     * POST alternative to 'list', which allows passing a much larger
     * list of ids to fetch via a JSON body.
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    public StreamingOutput listFromJson(String json)
            throws ItemNotFound, PermissionDenied, BadRequester, DeserializationError, IOException {
        return list(parseIds(json));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public StreamingOutput list(@QueryParam("id") List<String> ids) throws ItemNotFound,
            PermissionDenied, BadRequester {
        // Object a lazily-computed view of the ids->vertices...
        Iterable<Vertex> vertices = manager.getVertices(ids);
        PipeFunction<Vertex,Boolean> filter = aclManager
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                    .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
        return streamingVertexList(filtered, getSerializer());
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public StreamingOutput listByGidFromJson(String json) throws ItemNotFound,
            PermissionDenied, DeserializationError, BadRequester, IOException {

        return listByGid(parseGraphIds(json));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public StreamingOutput listByGid(@QueryParam("gid") List<Long> ids) throws ItemNotFound,
            PermissionDenied, BadRequester {
        // FIXME: This is ugly, but to return 404 on a bad item we have to
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

        PipeFunction<Vertex,Boolean> filter = aclManager
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
        return streamingVertexList(filtered, getSerializer());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/{id:.+}")
    public Response get(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester, SerializationError {
        // TODO: Make this more efficient - it's wasteful to use the
        // gremlin acl and type filtering for one item...
        List<String> ids = Lists.newArrayList(id);
        Iterable<Vertex> vertices = manager.getVertices(ids);
        PipeFunction<Vertex,Boolean> filter = aclManager
                .getAclFilterFunction(getRequesterUserProfile());
        GremlinPipeline<Vertex, Vertex> filtered = new GremlinPipeline<Vertex, Vertex>(
                vertices)
                .filter(aclManager.getContentTypeFilterFunction()).filter(filter);
        if (filtered.iterator().hasNext()) {
            return Response.status(Response.Status.OK)
                    .entity(getRepresentation(filtered.iterator().next()).getBytes())
                    .build();
        } else {
            throw new ItemNotFound(id);
        }
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
