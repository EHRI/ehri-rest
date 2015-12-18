/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.persistence;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing static methods to convert between FramedVertex instances,
 * EntityBundles, and raw data.
 */
public final class Serializer {

    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

    private static final int DEFAULT_CACHE_SIZE = 100;

    private static class LruCache<A, B> extends LinkedHashMap<A, B> {
        private final int maxEntries;

        public LruCache(int maxEntries) {
            super(maxEntries + 1, 1.0f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<A, B> eldest) {
            return super.size() > maxEntries;
        }
    }

    private final FramedGraph<?> graph;
    private final int maxTraversals;
    private final boolean dependentOnly;
    private final boolean liteMode;
    private final List<String> includeProps;
    private final LruCache<String, Bundle> cache;


    /**
     * Basic constructor.
     */
    public Serializer(FramedGraph<?> graph) {
        this(new Builder(graph));
    }

    /**
     * Builder for serializers with non-default options.
     */
    public static class Builder {
        private final FramedGraph<?> graph;
        private int maxTraversals = Fetch.DEFAULT_TRAVERSALS;
        private boolean dependentOnly;
        private boolean liteMode;
        private List<String> includeProps = Lists.newArrayList();
        private LruCache<String, Bundle> cache;

        public Builder(FramedGraph<?> graph) {
            this.graph = graph;
        }

        public Builder withDepth(int depth) {
            this.maxTraversals = depth;
            return this;
        }

        public Builder dependentOnly() {
            this.dependentOnly = true;
            return this;
        }

        public Builder dependentOnly(boolean dependentOnly) {
            this.dependentOnly = dependentOnly;
            return this;
        }

        public Builder withLiteMode(boolean lite) {
            this.liteMode = lite;
            return this;
        }

        public Builder withCache() {
            return withCache(DEFAULT_CACHE_SIZE);
        }

        public Builder withCache(int size) {
            this.cache = new LruCache<>(size);
            return this;
        }

        public Builder withIncludedProperties(List<String> properties) {
            this.includeProps = Lists.newArrayList(properties);
            return this;
        }

        public Serializer build() {
            return new Serializer(this);
        }

    }

    public Serializer(Builder builder) {
        this(builder.graph, builder.dependentOnly,
                builder.maxTraversals, builder.liteMode, builder.includeProps, builder.cache);
    }

    /**
     * Constructor which allows specifying whether to serialize non-dependent relations
     * and the level of traversal.
     *
     * @param graph         The framed graph
     * @param dependentOnly Only serialize dependent nodes
     * @param depth         Depth at which to stop recursion
     * @param lite          Only serialize mandatory properties
     * @param cache         Use a cache - use for single operations serializing many vertices
     *                      with common attributes, and NOT for reusable serializers
     */
    private Serializer(FramedGraph<?> graph, boolean dependentOnly, int depth, boolean lite,
            List<String> includeProps, LruCache<String, Bundle> cache) {
        this.graph = graph;
        this.dependentOnly = dependentOnly;
        this.maxTraversals = depth;
        this.liteMode = lite;
        this.includeProps = includeProps;
        this.cache = cache;
    }

    /**
     * Create a new serializer from this one, with extra included properties.
     *
     * @param includeProps A set of properties to include.
     * @return A new serializer.
     */
    public Serializer withIncludedProperties(List<String> includeProps) {
        return new Serializer(graph, dependentOnly, maxTraversals, liteMode,
                includeProps, cache);
    }

    /**
     * Get the list of included properties for this serializer.
     *
     * @return a list of property-name strings
     */
    public List<String> getIncludedProperties() {
        return includeProps;
    }

    /**
     * Return a serializer that caches recently-serialized items.
     * @return a new serializer
     */
    public Serializer withCache() {
        return new Builder(graph)
                .withIncludedProperties(includeProps)
                .withLiteMode(liteMode)
                .dependentOnly(dependentOnly)
                .withDepth(maxTraversals)
                .withCache().build();
    }

    /**
     * Convert a vertex frame to a raw bundle of data.
     *
     * @param item The framed item
     * @return A map of data
     * @throws SerializationError
     */
    public <T extends Frame> Map<String, Object> vertexFrameToData(T item)
            throws SerializationError {
        return vertexFrameToBundle(item).toData();
    }

    /**
     * Convert a vertex to a raw bundle of data.
     *
     * @param item The item vertex
     * @return A map of data
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
     * @param item The framed item
     * @return A data bundle
     * @throws SerializationError
     */
    public <T extends Frame> Bundle vertexFrameToBundle(T item)
            throws SerializationError {
        return vertexToBundle(item.asVertex(), 0, maxTraversals, false);
    }

    /**
     * Convert a Vertex into an EntityBundle that includes its @Fetch'd
     * relations.
     *
     * @param item The item vertex
     * @return A data bundle
     * @throws SerializationError
     */
    public Bundle vertexFrameToBundle(Vertex item)
            throws SerializationError {
        return vertexToBundle(item, 0, maxTraversals, false);
    }

    /**
     * Serialise a vertex frame to JSON.
     *
     * @param item The framed item
     * @return A JSON string
     * @throws SerializationError
     */
    public <T extends Frame> String vertexFrameToJson(T item)
            throws SerializationError {
        return DataConverter.bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex to JSON.
     *
     * @param item The item vertex
     * @return A JSON string
     * @throws SerializationError
     */
    public String vertexToJson(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML.
     *
     * @param item The item vertex
     * @return An XML document
     * @throws SerializationError
     */
    public Document vertexFrameToXml(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToXml(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML.
     *
     * @param item A framed item
     * @return An XML document
     * @throws SerializationError
     */
    public <T extends Frame> Document vertexFrameToXml(T item)
            throws SerializationError {
        return vertexFrameToXml(item.asVertex());
    }

    /**
     * Serialise a vertex frame to XML string.
     *
     * @param item The item vertex
     * @return An XML document string
     * @throws SerializationError
     */
    public String vertexToXmlString(Vertex item)
            throws SerializationError {
        return DataConverter.bundleToXmlString(vertexFrameToBundle(item));
    }

    /**
     * Serialise a vertex frame to XML string.
     *
     * @param item The framed item
     * @return An XML document string
     * @throws SerializationError
     */
    public <T extends Frame> String vertexFrameToXmlString(T item)
            throws SerializationError {
        return DataConverter.bundleToXmlString(vertexFrameToBundle(item));
    }

    /**
     * Run a callback every time a node in a subtree is encountered,
     * excepting the top-level node.
     *
     * @param item The item
     * @param cb   A callback object
     */
    public <T extends Frame> void traverseSubtree(T item,
            TraversalCallback cb) {
        traverseSubtree(item, 0, cb);
    }

    /**
     * Convert a Frame into an EntityBundle that includes its @Fetch'd
     * relations.
     *
     * @param item  The item vertex
     * @param depth The maximum serialization depth
     * @return A data bundle
     * @throws SerializationError
     */
    private Bundle vertexToBundle(Vertex item, int depth, int maxDepth, boolean lite)
            throws SerializationError {
        try {
            EntityClass type = EntityClass.withName((String) item
                    .getProperty(EntityType.TYPE_KEY));
            String id = item.getProperty(EntityType.ID_KEY);
            logger.trace("Serializing {} ({}) at depth {}", id, type, depth);

            Class<? extends Frame> cls = type.getJavaClass();
            Bundle.Builder builder = Bundle.Builder.withClass(type)
                    .setId(id)
                    .addData(getVertexData(item, type, lite))
                    .addRelations(getRelationData(item,
                            depth, maxDepth, lite, cls))
                    .addMetaData(getVertexMeta(item, cls));
            if (!lite) {
                builder.addMetaData(getVertexMeta(item, cls))
                        .addMetaDataValue("gid", item.getId());
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            logger.error("Error serializing vertex with data: {}", getVertexData(item));
            throw new SerializationError("Unable to serialize vertex: " + item, e);
        }
    }

    private Bundle fetch(Frame frame, int depth, int maxDepth, boolean isLite) throws SerializationError {
        if (cache != null) {
            String key = frame.getId() + depth + isLite;
            if (cache.containsKey(key))
                return cache.get(key);
            Bundle bundle = vertexToBundle(frame.asVertex(), depth, maxDepth, isLite);
            cache.put(key, bundle);
            return bundle;
        }
        return vertexToBundle(frame.asVertex(), depth, maxDepth, isLite);
    }

    // TODO: Profiling shows that (unsurprisingly) this method is a
    // performance hotspot. Rewrite it so that instead of using Frames
    // method invocations to do the traversal, we use regular traversals
    // whereever possible. Unfortunately the use of @JavaHandler Frame
    // annotations will make this difficult.
    private ListMultimap<String, Bundle> getRelationData(
            Vertex item, int depth, int maxDepth, boolean lite, Class<?> cls) {
        ListMultimap<String, Bundle> relations = ArrayListMultimap.create();
        if (depth < maxDepth) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            logger.trace(" - Fetch methods: {}", fetchMethods);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {
                String relationName = entry.getKey();
                Method method = entry.getValue();

                boolean isLite = liteMode || lite
                        || shouldSerializeLite(method);

                if (shouldTraverse(relationName, method, depth, isLite)) {
                    int nextDepth = depth + 1;
                    int nextMaxDepth = getNewMaxDepth(method, nextDepth, maxDepth);
                    logger.trace("Fetching relation: {}, depth {}, {}",
                            relationName, depth, method.getName());
                    try {
                        Object result = method.invoke(graph.frame(
                                item, cls));
                        // The result of one of these fetchMethods should either
                        // be a single Frame, or a Iterable<Frame>.
                        if (result instanceof Iterable<?>) {
                            for (Object d : (Iterable<?>) result) {
                                relations.put(relationName, fetch((Frame) d, nextDepth, nextMaxDepth, isLite));
                            }
                        } else {
                            // This relationship could be NULL if, e.g. a
                            // collection has no holder.
                            if (result != null) {
                                relations.put(relationName, fetch((Frame) result, nextDepth, nextMaxDepth, isLite));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("Error serializing relationship for {} ({}): {}, depth {}, {}",
                                item, item.getProperty(EntityType.TYPE_KEY),
                                relationName, depth, method.getName());
                        throw new RuntimeException(
                                "Unexpected error serializing Frame " + item, e);
                    }
                }
            }
        }
        return relations;
    }

    private int getNewMaxDepth(Method fetchMethod, int currentDepth, int currentMaxDepth) {
        Fetch fetchProps = fetchMethod.getAnnotation(Fetch.class);
        int max = fetchProps.numLevels();
        int newMax = max == -1
                ? currentMaxDepth
                : Math.min(currentDepth + max, currentMaxDepth);
        logger.trace("Current depth {}, fetch levels: {}, current max: {}, new max: {}, {}", currentDepth, max,
                currentMaxDepth, newMax, fetchMethod.getName());
        return newMax;
    }

    private boolean shouldSerializeLite(Method method) {
        Dependent dep = method.getAnnotation(Dependent.class);
        Fetch fetch = method.getAnnotation(Fetch.class);
        return dep == null && (fetch == null || !fetch.full());
    }

    private boolean shouldTraverse(String relationName, Method method, int level, boolean lite) {
        // In order to avoid @Fetching the whole graph we track the
        // depth parameter and increase it for every traversal.
        // However the @Fetch annotation can also specify a maximum
        // level of traversal beyond which we don't serialize.
        Fetch fetchProps = method.getAnnotation(Fetch.class);
        Dependent dep = method.getAnnotation(Dependent.class);

        if (fetchProps == null) {
            return false;
        }

        if (dependentOnly && dep == null) {
            logger.trace(
                    "Terminating fetch dependent only is specified: {}, ifBelowLevel {}, limit {}, {}",
                    relationName, level, fetchProps.ifBelowLevel());
            return false;
        }

        if (lite && fetchProps.whenNotLite()) {
            logger.trace(
                    "Terminating fetch because it specifies whenNotLite: {}, ifBelowLevel {}, limit {}, {}",
                    relationName, level, fetchProps.ifBelowLevel());
            return false;
        }

        if (level >= fetchProps.ifBelowLevel()) {
            logger.trace(
                    "Terminating fetch because level exceeded ifBelowLevel on fetch clause: {}, ifBelowLevel {}, " +
                            "limit {}, {}",
                    relationName, level, fetchProps.ifBelowLevel());
            return false;
        }

        // If the fetch should only be serialized at a certain ifBelowLevel and
        // we've exceeded that, don't serialize.
        if (fetchProps.ifLevel() != -1 && level > fetchProps.ifLevel()) {
            logger.trace(
                    "Terminating fetch because ifLevel clause found on {}, ifBelowLevel {}, {}",
                    relationName, level);
            return false;
        }
        return true;
    }

    /**
     * Fetch a map of data from a vertex.
     */
    private Map<String, Object> getVertexData(Vertex item, EntityClass type, boolean lite) {
        Map<String, Object> data = Maps.newHashMap();
        Iterable<String> keys = lite
                ? getMandatoryOrSpecificProps(type)
                : item.getPropertyKeys();

        for (String key : keys) {
            if (!(key.equals(EntityType.ID_KEY) || key
                    .equals(EntityType.TYPE_KEY) || key.startsWith("_")))
                data.put(key, item.getProperty(key));
        }
        return data;
    }

    /**
     * Get a list of properties with are either given specifically
     * in this serializer's includeProps attr, or are mandatory for
     * the type.
     *
     * @param type An EntityClass
     * @return A list of mandatory or included properties.
     */
    private List<String> getMandatoryOrSpecificProps(EntityClass type) {
        return Lists.newArrayList(
                Iterables.concat(ClassUtils.getMandatoryPropertyKeys(type.getJavaClass()),
                        includeProps));
    }

    /**
     * Fetch a map of metadata sourced from vertex properties.
     * This is anything that begins with an underscore (but now
     * two underscores)
     */
    private Map<String, Object> getVertexMeta(Vertex item, Class<?> cls) {
        Map<String, Object> data = Maps.newHashMap();
        for (String key : item.getPropertyKeys()) {
            if (!key.startsWith("__") && key.startsWith("_")) {
                data.put(key.substring(1), item.getProperty(key));
            }
        }
        Map<String, Method> metaMethods = ClassUtils.getMetaMethods(cls);
        if (!metaMethods.isEmpty()) {
            try {
                Object frame = graph.frame(item, cls);
                for (Map.Entry<String, Method> metaEntry : metaMethods.entrySet()) {
                    Object value = metaEntry.getValue().invoke(frame);
                    if (value != null) {
                        data.put(metaEntry.getKey(), value);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching metadata", e);
            }
        }
        return data;
    }

    private Map<String, Object> getVertexData(Vertex item) {
        Map<String, Object> data = Maps.newHashMap();
        for (String key : item.getPropertyKeys()) {
            data.put(key, item.getProperty(key));
        }
        return data;
    }

    /**
     * Run a callback every time a node in a subtree is encountered, excepting
     * the top-level node.
     */
    private <T extends Frame> void traverseSubtree(T item, int depth,
            TraversalCallback cb) {

        if (depth < maxTraversals) {
            Class<?> cls = EntityClass
                    .withName(item.<String>getProperty(EntityType.TYPE_KEY)).getJavaClass();
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
