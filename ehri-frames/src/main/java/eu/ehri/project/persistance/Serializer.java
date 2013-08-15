package eu.ehri.project.persistance;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.base.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.utils.ClassUtils;
import org.w3c.dom.Document;

/**
 * Class containing static methods to convert between FramedVertex instances,
 * EntityBundles, and raw data.
 * 
 * @author michaelb
 * 
 */
public final class Serializer {

    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

    private final FramedGraph<?> graph;

    /**
     * Lookup of entityType keys against their annotated class.
     */
    private final int maxTraversals;
    private final boolean dependentOnly;
    private final boolean liteMode;

    /**
     * Constructor.
     */
    public Serializer(FramedGraph<?> graph) {
        this(graph, false, Fetch.DEFAULT_TRAVERSALS, false);
    }

    /**
     * Constructor which allows specifying depth of @Fetched traversals.
     * 
     * @param depth
     */
    public Serializer(FramedGraph<?> graph, int depth) {
        this(graph, false, depth, false);
    }

    /**
     * Constructor which allows specifying whether to serialize non-dependent relations.
     *
     * @param dependentOnly
     */
    public Serializer(FramedGraph<?> graph, boolean dependentOnly) {
        this(graph, dependentOnly, Fetch.DEFAULT_TRAVERSALS, false);
    }

    /**
     * Constructor which allows specifying whether to serialize non-dependent relations
     * and the depth of traversal.
     *
     * @param dependentOnly Only serialize dependent nodes
     * @param depth Depth at which to stop recursion
     * @param lite  Only serialize mandatory properties
     */
    public Serializer(FramedGraph<?> graph, boolean dependentOnly, int depth, boolean lite) {
        this.graph = graph;
        this.dependentOnly = dependentOnly;
        this.maxTraversals = depth;
        this.liteMode = lite;
    }

    /**
     * Factory method for obtaining a default serializer for
     * the given graph.
     *
     * @param graph The framed graph
     */
    public static Serializer defaultSerializer(FramedGraph<?> graph) {
        return new Serializer(graph, false, Fetch.DEFAULT_TRAVERSALS, false);
    }

    /**
     * Factory method for obtaining a serializer which only writes
     * mandatory properties.
     *
     * @param graph The framed graph
     */
    public static Serializer liteSerializer(FramedGraph<?> graph) {
        return new Serializer(graph, false, Fetch.DEFAULT_TRAVERSALS, true);
    }

    /**
     * Factory method for obtaining a serializer which writes to
     * the specified depth.
     *
     * @param depth Maximum depth of traversal.
     *
     * @param graph The framed graph
     */
    public static Serializer depthSerializer(FramedGraph<?> graph, int depth) {
        return new Serializer(graph, false, depth, false);
    }

    /**
     * Convert a vertex frame to a raw bundle of data.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends Frame> Map<String, Object> vertexFrameToData(T item)
            throws SerializationError {
        return vertexFrameToBundle(item).toData();
    }

    /**
     * Convert a vertex to a raw bundle of data.
     *
     * @param item
     * @return
     * @throws SerializationError
     */
    public Map<String, Object> vertexToData(Vertex item)
            throws SerializationError {
        return vertexFrameToBundle(item).toData();
    }

    /**
     * Convert a Frame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends Frame> Bundle vertexFrameToBundle(T item)
            throws SerializationError {
        return vertexToBundle(item.asVertex(), 0, false);
    }

    /**
     * Convert a Vertex into an EntityBundle that includes its @Fetch'd
     * relations.
     *
     * @param item
     * @return
     * @throws SerializationError
     */
    public  Bundle vertexFrameToBundle(Vertex item)
            throws SerializationError {
        return vertexToBundle(item, 0, false);
    }

    /**
     * Serialise a vertex frame to JSON.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends Frame> String vertexFrameToJson(T item)
            throws SerializationError {
        return DataConverter.bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex to JSON.
     *
     * @param item
     * @return
     * @throws SerializationError
     */
    public String vertexToJson(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML.
     *
     * @param item
     * @return document
     * @throws SerializationError
     */
    public Document vertexFrameToXml(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToXml(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML.
     *
     * @param item
     * @return document
     * @throws SerializationError
     */
    public <T extends Frame> Document vertexFrameToXml(T item)
            throws SerializationError {
        return DataConverter.bundleToXml(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML string.
     *
     * @param item
     * @return document string
     * @throws SerializationError
     */
    public String vertexToXmlString(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToXmlString(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML string.
     *
     * @param item
     * @return document string
     * @throws SerializationError
     */
    public <T extends Frame> String vertexFrameToXmlString(T item)
            throws SerializationError {
        return DataConverter.bundleToXmlString(vertexFrameToBundle(item));
    }

    /**
     * Run a callback every time a node in a subtree is encountered, starting
     * with the top-level node.
     * 
     * @param item
     * @param cb
     */
    public <T extends Frame> void traverseSubtree(T item,
            final TraversalCallback cb) {
        traverseSubtree(item, 0, cb);
    }

    /**
     * Convert a Frame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @param depth
     * @return
     * @throws SerializationError
     */
    private Bundle vertexToBundle(Vertex item, int depth, boolean lite)
            throws SerializationError {
        // FIXME: Try and move the logic for accessing id and type elsewhere.
        try {
            String id = (String) item.getProperty(EntityType.ID_KEY);
            EntityClass type = EntityClass.withName((String) item
                    .getProperty(EntityType.TYPE_KEY));
            logger.trace("Serializing {} ({}) at depth {}", id, type, depth);
            ListMultimap<String, Bundle> relations = getRelationData(item,
                    depth, lite, type.getEntityClass());
            Map<String, Object> data = getVertexData(item, type, depth, lite);
            return new Bundle(id, type, data,
                    relations);
        } catch (IllegalArgumentException e) {
            throw new SerializationError("Unable to serialize vertex: " + item,
                    e);
        }
    }

    // TODO: Profiling shows that (unsurprisingly) this method is a
    // performance hotspot. Rewrite it so that instead of using Frames
    // method invocations to do the traversal, we use regular traversals
    // whereever possible. Unfortunately the use of @JavaHandler Frame
    // annotations will make this difficult.
    private ListMultimap<String, Bundle> getRelationData(
            Vertex item, int depth, boolean lite, Class<?> cls) {
        ListMultimap<String, Bundle> relations = LinkedListMultimap.create();
        if (depth < maxTraversals) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            logger.trace(" - Fetch methods: {}", fetchMethods);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {
                String relationName = entry.getKey();
                Method method = entry.getValue();

                boolean isLite = liteMode || lite
                        || shouldSerializeLite(relationName, method);

                if (shouldTraverse(relationName, method, depth, isLite)) {
                    logger.trace("Fetching relation: {}, depth {}, {}",
                            relationName, depth, method.getName());
                    try {
                        Object result = method.invoke(graph.frame(
                                item, cls));
                        // The result of one of these fetchMethods should either
                        // be a single Frame, or a Iterable<Frame>.
                        if (result instanceof Iterable<?>) {
                            for (Object d : (Iterable<?>) result) {
                                relations.put(
                                        relationName,
                                        vertexToBundle(((Frame) d).asVertex(),
                                                depth + 1, isLite));
                            }
                        } else {
                            // This relationship could be NULL if, e.g. a
                            // collection has no holder.
                            if (result != null)
                                relations
                                        .put(relationName,
                                                vertexToBundle(
                                                        ((Frame) result).asVertex(),
                                                        depth + 1, isLite));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(
                                "Unexpected error serializing Frame", e);
                    }
                }
            }
        }
        return relations;
    }

    /**
     * Determine if a relation should be serialized without its non-mandatory
     * data. This will be the case if:
     *
     *  - depth is > 0
     *  - item is a non-dependent relationship
     *
     * @param relationName
     * @param method
     * @return
     */
    private boolean shouldSerializeLite(String relationName, Method method) {
        Dependent dep = method.getAnnotation(Dependent.class);
        return dep == null;
    }

    /**
     * Determine if traversal should proceed on a Frames relation.
     * 
     * @param relationName
     * @param method
     * @param depth
     * @return
     */
    private boolean shouldTraverse(String relationName, Method method, int depth, boolean lite) {
        // In order to avoid @Fetching the whole graph we track the
        // depth parameter and increase it for every traversal.
        // However the @Fetch annotation can also specify a maximum
        // depth of traversal beyong which we don't serialize.
        Fetch fetchProps = method.getAnnotation(Fetch.class);
        Dependent dep = method.getAnnotation(Dependent.class);

        if (fetchProps == null)
            return false;

        if (dependentOnly && dep == null) {
            logger.trace(
                    "Terminating fetch dependent only is specified: {}, depth {}, limit {}, {}",
                    relationName, depth, fetchProps.depth());
            return false;
        }

        if (lite && fetchProps.whenNotLite()) {
            logger.trace(
                    "Terminating fetch because it specifies whenNotLite: {}, depth {}, limit {}, {}",
                    relationName, depth, fetchProps.depth());
            return false;
        }

        if (depth >= fetchProps.depth()) {
            logger.trace(
                    "Terminating fetch because depth exceeded depth on fetch clause: {}, depth {}, limit {}, {}",
                    relationName, depth, fetchProps.depth());
            return false;
        }

        // If the fetch should only be serialized at a certain depth and
        // we've exceeded that, don't serialize.
        if (fetchProps.ifDepth() != -1 && depth > fetchProps.ifDepth()) {
            logger.trace(
                    "Terminating fetch because ifDepth clause found on {}, depth {}, {}",
                    relationName, depth);
            return false;
        }
        return true;
    }

    /**
     * Fetch a map of data from a vertex.
     */
    private Map<String, Object> getVertexData(Vertex item, EntityClass type, int depth, boolean lite) {
        Map<String, Object> data = Maps.newHashMap();
        Iterable<String> keys = lite
                ? ClassUtils.getMandatoryPropertyKeys(type.getEntityClass())
                : item.getPropertyKeys();

        for (String key : keys) {
            if (!(key.equals(EntityType.ID_KEY) || key
                    .equals(EntityType.TYPE_KEY) || key.startsWith("_")))
                data.put(key, item.getProperty(key));
        }
        return data;
    }

    /**
     * Run a callback every time a node in a subtree is encountered, starting
     * with the top-level node.
     * 
     * @param item
     * @param depth
     * @param cb
     */
    private <T extends Frame> void traverseSubtree(T item, int depth,
            final TraversalCallback cb) {

        if (depth < maxTraversals) {
            Class<?> cls = EntityClass.withName(
                    (String) item.asVertex().getProperty(EntityType.TYPE_KEY))
                    .getEntityClass();
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {

                String relationName = entry.getKey();
                Method method = entry.getValue();
                if (shouldTraverse(relationName, method, depth, false)) {
                    try {
                        Object result = method.invoke(graph.frame(
                                item.asVertex(), cls));
                        if (result instanceof Iterable<?>) {
                            int rnum = 0;
                            for (Object d : (Iterable<?>) result) {
                                cb.process((Frame) d, depth,
                                        entry.getKey(), rnum);
                                traverseSubtree((Frame) d, depth + 1, cb);
                                rnum++;
                            }
                        } else {
                            if (result != null) {
                                cb.process((Frame) result, depth,
                                        entry.getKey(), 0);
                                traverseSubtree((Frame) result,
                                        depth + 1, cb);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(
                                "Unexpected error serializing Frame", e);
                    }
                }
            }
        }
    }
}
