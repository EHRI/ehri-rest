package eu.ehri.project.persistance;

import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
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

    /**
     * Constructor.
     */
    public Serializer(FramedGraph<?> graph) {
        this(graph, Fetch.DEFAULT_TRAVERSALS);
    }

    /**
     * Constructor which allows specifying depth of @Fetched traversals.
     * 
     * @param depth
     */
    public Serializer(FramedGraph<?> graph, int depth) {
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
        return vertexToBundle(item.asVertex(), 0);
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
        return vertexToBundle(item, 0);
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
    private Bundle vertexToBundle(Vertex item, int depth)
            throws SerializationError {
        // FIXME: Try and move the logic for accessing id and type elsewhere.
        try {
            String id = (String) item.getProperty(EntityType.ID_KEY);
            EntityClass type = EntityClass.withName((String) item
                    .getProperty(EntityType.TYPE_KEY));
            logger.trace("Serializing {} ({}) at depth {}", id, type, depth);
            ListMultimap<String, Bundle> relations = getRelationData(item,
                    depth, type.getEntityClass());
            return new Bundle(id, type, getVertexData(item, type, depth),
                    relations);
        } catch (IllegalArgumentException e) {
            throw new SerializationError("Unable to serialize vertex: " + item,
                    e);
        }
    }

    // TODO: Profiling shows that (unsurprisingly) this method is a
    // performance hotspot. Rewrite it so that instead of using Frames
    // method invocations to do the traversal, we use regular traversals
    // whereever possible. Unfortunately the use of @GremlinGroovy Frame
    // annotations will make this difficult.
    private ListMultimap<String, Bundle> getRelationData(
            Vertex item, int depth, Class<?> cls) {
        ListMultimap<String, Bundle> relations = LinkedListMultimap.create();
        if (depth < maxTraversals) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            logger.trace(" - Fetch methods: {}", fetchMethods);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {
                String relationName = entry.getKey();
                Method method = entry.getValue();

                if (shouldTraverse(relationName, method, depth)) {
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
                                                depth + 1));
                            }
                        } else {
                            // This relationship could be NULL if, e.g. a
                            // collection has no holder.
                            if (result != null)
                                relations
                                        .put(relationName,
                                                vertexToBundle(
                                                        ((Frame) result).asVertex(),
                                                        depth + 1));
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
     * Determine if traversal should proceed on a Frames relation.
     * 
     * @param relationName
     * @param method
     * @param depth
     * @return
     */
    private boolean shouldTraverse(String relationName, Method method, int depth) {
        // In order to avoid @Fetching the whole graph we track the
        // depth parameter and increase it for every traversal.
        // However the @Fetch annotation can also specify a maximum
        // depth of traversal beyong which we don't serialize.
        Fetch fetchProps = method.getAnnotation(Fetch.class);
        if (fetchProps == null)
            return false;

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
    private Map<String, Object> getVertexData(Vertex item, EntityClass type, int depth) {
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
                if (shouldTraverse(relationName, method, depth)) {
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
