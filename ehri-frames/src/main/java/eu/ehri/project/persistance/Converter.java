package eu.ehri.project.persistance;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.utils.ClassUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Class containing static methods to convert between FramedVertex instances,
 * EntityBundles, and raw data.
 * 
 * @author michaelb
 * 
 */
public final class Converter {

    private final FramedGraph<Neo4jGraph> graph;
    public static final int DEFAULT_TRAVERSALS = 5;

    /**
     * Lookup of entityType keys against their annotated class.
     */
    private final int maxTraversals;

    /**
     * Constructor.
     */
    public Converter(FramedGraph<Neo4jGraph> graph) {
        this(graph, DEFAULT_TRAVERSALS);
    }

    /**
     * Constructor which allows specifying depth of @Fetched traversals.
     * 
     * @param depth
     */
    public Converter(FramedGraph<Neo4jGraph> graph, int depth) {
        this.graph = graph;
        this.maxTraversals = depth;
    }

    /**
     * Convert a vertex frame to a raw bundle of data.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> Map<String, Object> vertexFrameToData(T item)
            throws SerializationError {
        return bundleToData(vertexFrameToBundle(item));
    }

    /**
     * Convert some JSON into an EntityBundle.
     * 
     * @param json
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @throws DeserializationError
     */
    public Bundle jsonToBundle(String json) throws DeserializationError {
        return DataConverter.jsonToBundle(json);
    }

    /**
     * Serialise a vertex frame to JSON.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> String vertexFrameToJson(T item)
            throws SerializationError {
        return DataConverter.bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Convert a bundle to JSON.
     * 
     * @param bundle
     * @return
     * @throws SerializationError
     * 
     * @deprecated Use DataConverter static method instead.
     * 
     */
    public String bundleToJson(Bundle bundle) throws SerializationError {
        return DataConverter.bundleToJson(bundle);
    }

    /**
     * Convert generic data into a bundle.
     * 
     * Prize to whomever can remove all the unchecked warnings. I don't really
     * know how else to do this otherwise.
     * 
     * @throws DeserializationError
     */
    public Bundle dataToBundle(Map<String, Object> data)
            throws DeserializationError {
        return DataConverter.dataToBundle(data);
    }

    /**
     * Convert a bundle to a generic data structure.
     * 
     * @param bundle
     * @return
     */
    public Map<String, Object> bundleToData(Bundle bundle) {
        return DataConverter.bundleToData(bundle);
    }

    /**
     * Convert a VertexFrame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> Bundle vertexFrameToBundle(T item)
            throws SerializationError {
        return vertexFrameToBundle(item, maxTraversals);
    }

    /**
     * Convert a VertexFrame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @param depth
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> Bundle vertexFrameToBundle(T item, int depth)
            throws SerializationError {
        // FIXME: Try and move the logic for accessing id and type elsewhere.
        try {
            String id = (String) item.asVertex().getProperty(EntityType.ID_KEY);
            EntityClass type = EntityClass.withName((String) item.asVertex()
                    .getProperty(EntityType.TYPE_KEY));
            ListMultimap<String, Bundle> relations = getRelationData(item,
                    depth, type.getEntityClass());
            return new Bundle(id, type, getVertexData(item.asVertex()),
                    relations);
        } catch (IllegalArgumentException e) {
            throw new SerializationError("Unable to serialize vertex: " + item,
                    e);
        }
    }

    private <T extends VertexFrame> ListMultimap<String, Bundle> getRelationData(
            T item, int depth, Class<?> cls) {
        ListMultimap<String, Bundle> relations = LinkedListMultimap.create();
        if (depth > 0) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {

                // In order to avoid @Fetching the whole graph we track the
                // maxDepth parameter and reduce it for every traversal.
                // However the @Fetch annotation can also specify a non-default
                // depth, so we need to determine whatever is lower - the
                // current traversal count, or the annotation's count.
                Method method = entry.getValue();
                int nextDepth = Math.min(depth,
                        method.getAnnotation(Fetch.class).depth()) - 1;

                try {
                    Object result;
                    try {
                        // NB: We have to re-cast the item into its 'natural'
                        // type.
                        result = method
                                .invoke(graph.frame(item.asVertex(), cls));
                    } catch (IllegalArgumentException e) {
                        String message = String
                                .format("When serializing a bundle, a method was called on an item it did not expect. Method name: %s, item class: %s",
                                        method.getName(),
                                        item.asVertex().getProperty(
                                                EntityType.TYPE_KEY));
                        throw new RuntimeException(message, e);
                    }
                    // The result of one of these fetchMethods should either be
                    // a single VertexFrame, or a Iterable<VertexFrame>.
                    if (result instanceof Iterable<?>) {
                        for (Object d : (Iterable<?>) result) {
                            relations.put(
                                    entry.getKey(),
                                    vertexFrameToBundle((VertexFrame) d,
                                            nextDepth));
                        }
                    } else {
                        // This relationship could be NULL if, e.g. a collection
                        // has no holder.
                        if (result != null)
                            relations.put(
                                    entry.getKey(),
                                    vertexFrameToBundle((VertexFrame) result,
                                            nextDepth));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(
                            "Unexpected error serializing VertexFrame", e);
                }
            }
        }
        return relations;
    }

    /**
     * Fetch a map of data from a vertex.
     */
    private Map<String, Object> getVertexData(Vertex item) {
        Map<String, Object> data = Maps.newHashMap();
        for (String key : item.getPropertyKeys()) {
            if (!(key.equals(EntityType.ID_KEY) || key
                    .equals(EntityType.TYPE_KEY)))
                data.put(key, item.getProperty(key));
        }
        return data;
    }
}
