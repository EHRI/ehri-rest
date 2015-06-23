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

package eu.ehri.project.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.WrappingCloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.models.utils.EmptyIterable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes.
 *
 * This class can be extended for when specific graph implementations (such
 * as Neo4j) can provide more efficient implementations of certain methods.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class BlueprintsGraphManager<T extends IndexableGraph> implements GraphManager {

    protected static final String INDEX_NAME = "entities";
    protected static final String METADATA_PREFIX = "_";

    protected final FramedGraph<T> graph;

    public FramedGraph<T> getGraph() {
        return graph;
    }

    public BlueprintsGraphManager(FramedGraph<T> graph) {
        this.graph = graph;
    }

    @Override
    public <E extends Frame> E cast(Frame frame, Class<E> cls) {
        return graph.frame(frame.asVertex(), cls);
    }

    @Override
    public String getId(Vertex vertex) {
        return vertex.getProperty(EntityType.ID_KEY);
    }

    @Override
    public String getType(Vertex vertex) {
        return vertex.getProperty(EntityType.TYPE_KEY);
    }

    @Override
    public Map<String,Object> getProperties(Vertex vertex) {
        Map<String,Object> props = Maps.newHashMap();
        for (String key : vertex.getPropertyKeys()) {
            if (!key.startsWith(METADATA_PREFIX)) {
                props.put(key, vertex.getProperty(key));
            }
        }
        return props;
    }

    @Override
    public EntityClass getEntityClass(Vertex vertex) {
        Preconditions.checkNotNull(vertex);
        return EntityClass.withName(getType(vertex));
    }

    @Override
    public EntityClass getEntityClass(Frame frame) {
        Preconditions.checkNotNull(frame);
        return EntityClass.withName(frame.getType());
    }

    @Override
    public boolean exists(String id) {
        Preconditions.checkNotNull(id,
                "attempt determine existence of a vertex with a null id");
        return getIndex().count(EntityType.ID_KEY, id) > 0L;
    }

    @Override
    public <E> E getFrame(String id, Class<E> cls) throws ItemNotFound {
        return graph.frame(getVertex(id), cls);
    }

    @Override
    public <E> E getFrame(String id, EntityClass type, Class<E> cls)
            throws ItemNotFound {
        return graph.frame(getVertex(id, type), cls);
    }

    @Override
    public <E> CloseableIterable<E> getFrames(EntityClass type, Class<E> cls) {
        return new WrappingCloseableIterable<>(
                graph.frameVertices(getVertices(type), cls));
    }

    @Override
    public Vertex getVertex(String id) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        try (CloseableIterable<Vertex> query = getIndex().get(EntityType.ID_KEY, id)) {
            return query.iterator().next();
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        }
    }

    @Override
    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        for (Vertex v : graph.getVertices(EntityType.ID_KEY, id)) {
            if (getEntityClass(v).equals(type))
                return v;
        }
        throw new ItemNotFound(id);
    }

    @Override
    public CloseableIterable<Vertex> getVertices(EntityClass type) {
        return getIndex().get(EntityType.TYPE_KEY, type.getName());
    }

    @Override
    public CloseableIterable<Vertex> getVertices(Iterable<String> ids) throws ItemNotFound {
        // Ugh, we don't want to remove duplicate results here
        // because that's not expected behaviour - if you give
        // an array with dups you expect the dups to come out...
        List<Vertex> verts = Lists.newArrayList();
        for (String id : ids) {
            verts.add(getVertex(id));
        }
        return new WrappingCloseableIterable<>(verts);
    }

    @Override
    public CloseableIterable<Vertex> getVertices(String key, Object value, EntityClass type) {
        // NB: This is rather annoying.
        List<Vertex> elems = Lists.newArrayList();
        try (CloseableIterable<Vertex> query = getIndex().get(key, value)) {
            for (Vertex v : query) {
                if (getEntityClass(v).equals(type)) {
                    elems.add(v);
                }
            }
        }
        return new WrappingCloseableIterable<>(elems);
    }

    @Override
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data) throws IntegrityError {
        return createVertex(id, type, data, data.keySet());
    }

    @Override
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws IntegrityError {
        Preconditions
                .checkNotNull(id, "null vertex ID given for item creation");
        Index<Vertex> index = getIndex();
        Map<String, ?> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        checkExists(index, id);
        Vertex node = graph.addVertex(null);
        for (Map.Entry<String, ?> entry : indexData.entrySet()) {
            if (entry.getValue() == null)
                continue;
            node.setProperty(entry.getKey(), entry.getValue());
            if (keys == null || indexKeys.contains(entry.getKey())) {
                index.put(entry.getKey(), String.valueOf(entry.getValue()),
                        node);
            }
        }
        return node;
    }

    @Override
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data) throws ItemNotFound {
        return updateVertex(id, type, data, data.keySet());
    }

    @Override
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws ItemNotFound {
        Preconditions.checkNotNull(id, "null vertex ID given for item update");
        Index<Vertex> index = getIndex();
        Map<String, ?> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        try (CloseableIterable<Vertex> get = getIndex().get(EntityType.ID_KEY, id)) {
            try {
                Vertex node = get.iterator().next();
                replaceProperties(index, node, indexData, indexKeys);
                return node;
            } catch (NoSuchElementException e) {
                throw new ItemNotFound(id);
            }
        }
    }

    @Override
    public void setProperty(Vertex vertex, String key, Object value) {
        Preconditions.checkNotNull(vertex);
        Preconditions.checkNotNull(key);
        Preconditions.checkArgument(
                !(key.trim().isEmpty() ||
                        key.equals(EntityType.ID_KEY) || key.equals(EntityType.TYPE_KEY)),
                "Invalid property key: %s", key);
        Index<Vertex> index = getIndex();
        Object current = vertex.getProperty(key);
        if (current != null) {
            index.remove(key, current, vertex);
        }
        if (value == null) {
            vertex.removeProperty(key);
        } else {
            vertex.setProperty(key, value);
            index.put(key, value, vertex);
        }
    }

    @Override
    public void renameVertex(Vertex vertex, String oldId, String newId) {
        Preconditions.checkNotNull(vertex);
        Preconditions.checkNotNull(newId);
        Index<Vertex> index = getIndex();
        index.remove(EntityType.ID_KEY, oldId, vertex);
        vertex.setProperty(EntityType.ID_KEY, newId);
        index.put(EntityType.ID_KEY, newId, vertex);
    }

    @Override
    public void deleteVertex(String id) throws ItemNotFound {
        deleteVertex(getVertex(id));
    }

    @Override
    public void deleteVertex(Vertex vertex) {
        Index<Vertex> index = getIndex();
        for (String key : vertex.getPropertyKeys()) {
            index.remove(key, vertex.getProperty(key), vertex);
        }
        vertex.remove();
    }

    @Override
    public void rebuildIndex() {
        graph.getBaseGraph().dropIndex(INDEX_NAME);
        Index<Vertex> index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
        // index vertices
        for (Vertex vertex : graph.getVertices()) {
            reindex(index, vertex);
        }
    }

    private <E extends Element> void replaceProperties(Index<E> index, E item,
            Map<String, ?> data, Collection<String> keys) {
        // remove 'old' properties
        for (String key : item.getPropertyKeys()) {
            Object value = item.getProperty(key);
            if (!key.startsWith(METADATA_PREFIX)) {
                item.removeProperty(key);
                if (keys == null || keys.contains(key)) {
                    index.remove(key, value, item);
                }
            }
        }

        // add all 'new' properties to the relationship and index
        addProperties(index, item, data, keys);
    }

    private <E extends Element> void addProperties(Index<E> index, E item,
            Map<String, ?> data, Collection<String> keys) {
        Preconditions.checkNotNull(data, "Data map cannot be null");
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            item.setProperty(entry.getKey(), entry.getValue());
            if (keys == null || keys.contains(entry.getKey()))
                index.put(entry.getKey(), String.valueOf(entry.getValue()),
                        item);
        }
    }

    private void checkExists(Index<Vertex> index, String id)
            throws IntegrityError {
        if (index.count(EntityType.ID_KEY, id) != 0) {
            throw new IntegrityError(id);
        }
    }

    private Map<String, ?> getVertexData(String id, EntityClass type,
            Map<String, ?> data) {
        Map<String, Object> vdata = Maps.newHashMap(data);
        vdata.put(EntityType.ID_KEY, id);
        vdata.put(EntityType.TYPE_KEY, type.getName());
        return vdata;
    }

    private Collection<String> getVertexKeys(Iterable<String> keys) {
        List<String> vkeys = Lists.newArrayList(keys);
        vkeys.add(EntityType.ID_KEY);
        vkeys.add(EntityType.TYPE_KEY);
        return vkeys;
    }

    private Index<Vertex> getIndex() {
        Index<Vertex> index = graph.getBaseGraph().getIndex(INDEX_NAME,
                Vertex.class);
        if (index == null) {
            index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
        }
        return index;
    }

    private void reindex(Index<Vertex> index, Vertex vertex) {
        for (String key : propertyKeysToIndex(vertex)) {
            Object val = vertex.getProperty(key);
            if (val != null) {
                index.put(key, val, vertex);
            }
        }
    }

    private static Iterable<String> propertyKeysToIndex(Vertex vertex) {
        String typeName = vertex.getProperty(EntityType.TYPE_KEY);
        try {
            EntityClass entityClass = EntityClass.withName(typeName);
            return ClassUtils.getPropertyKeys(entityClass.getEntityClass());
        } catch (Exception e) {
            return new EmptyIterable<>();
        }
    }
}
