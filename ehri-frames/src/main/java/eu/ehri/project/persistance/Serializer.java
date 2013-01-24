package eu.ehri.project.persistance;

import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class containing static methods to convert between FramedVertex instances,
 * EntityBundles, and raw data.
 * 
 * @author michaelb
 * 
 */
public final class Serializer {

    private final FramedGraph<Neo4jGraph> graph;

    /**
     * Lookup of entityType keys against their annotated class.
     */
    private final int maxTraversals;

    /**
     * Constructor.
     */
    public Serializer(FramedGraph<Neo4jGraph> graph) {
        this(graph, Fetch.DEFAULT_TRAVERSALS);
    }

    /**
     * Constructor which allows specifying depth of @Fetched traversals.
     * 
     * @param depth
     */
    public Serializer(FramedGraph<Neo4jGraph> graph, int depth) {
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
        return vertexFrameToBundle(item).toData();
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
        return vertexFrameToBundle(item, 0);
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
     * Convert a VertexFrame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @param depth
     * @return
     * @throws SerializationError
     */
    private <T extends VertexFrame> Bundle vertexFrameToBundle(T item, int depth)
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
        if (depth < maxTraversals) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {

                // In order to avoid @Fetching the whole graph we track the
                // maxDepth parameter and reduce it for every traversal.
                // However the @Fetch annotation can also specify a non-default
                // depth, so we need to determine whatever is lower - the
                // current traversal count, or the annotation's count.
                Method method = entry.getValue();
                int nextDepth = Math.max(depth,
                        method.getAnnotation(Fetch.class).depth()) + 1;

                try {
                    Object result = method
                            .invoke(graph.frame(item.asVertex(), cls));
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
    
    /**
     * Run a callback every time a node in a subtree is encountered, starting
     * with the top-level node.
     * 
     * @param item
     * @param cb
     */
    public <T extends VertexFrame> void traverseSubtree(
            T item, final TraversalCallback cb) {
        traverseSubtree(item, 0, cb);
    }
    
    /**
     * Run a callback every time a node in a subtree is encountered, starting
     * with the top-level node.
     * 
     * @param item
     * @param depth
     * @param cb
     */
    private <T extends VertexFrame> void traverseSubtree(
            T item, int depth, final TraversalCallback cb) {
        
        if (depth < maxTraversals) {
            Class<?> cls = EntityClass.withName((String) item.asVertex()
                    .getProperty(EntityType.TYPE_KEY)).getEntityClass();
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {

                // In order to avoid @Fetching the whole graph we track the
                // maxDepth parameter and reduce it for every traversal.
                // However the @Fetch annotation can also specify a non-default
                // depth, so we need to determine whatever is lower - the
                // current traversal count, or the annotation's count.
                Method method = entry.getValue();
                int nextDepth = Math.max(depth,
                        method.getAnnotation(Fetch.class).depth()) + 1;

                try {
                    Object result = method
                            .invoke(graph.frame(item.asVertex(), cls));
                    // The result of one of these fetchMethods should either be
                    // a single VertexFrame, or a Iterable<VertexFrame>.
                    if (result instanceof Iterable<?>) {
                        int rnum = 0;
                        for (Object d : (Iterable<?>) result) {
                            cb.process((VertexFrame)d, depth, entry.getKey(), rnum);
                            traverseSubtree((VertexFrame)d, nextDepth, cb);
                            rnum++;
                        }
                    } else {
                        // This relationship could be NULL if, e.g. a collection
                        // has no holder.
                        if (result != null) {
                            cb.process((VertexFrame)result, depth, entry.getKey(), 0);
                            traverseSubtree((VertexFrame)result, nextDepth, cb);                            
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(
                            "Unexpected error serializing VertexFrame", e);
                }
            }
        }
    }    
}
