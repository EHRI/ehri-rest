/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.WrappingCloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Entity;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes.
 * <p>
 * This class can be extended for when specific graph implementations (such
 * as Neo4j) can provide more efficient implementations of certain methods.
 */
public class BlueprintsGraphManager<T extends Graph> implements GraphManager {

    protected static final String METADATA_PREFIX = "_";

    protected final FramedGraph<T> graph;

    public FramedGraph<T> getGraph() {
        return graph;
    }

    public BlueprintsGraphManager(FramedGraph<T> graph) {
        this.graph = graph;
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
    public Map<String, Object> getProperties(Vertex vertex) {
        Map<String, Object> props = Maps.newHashMap();
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
    public EntityClass getEntityClass(Entity entity) {
        Preconditions.checkNotNull(entity);
        return EntityClass.withName(entity.getType());
    }

    @Override
    public boolean exists(String id) {
        Preconditions.checkNotNull(id,
                "attempt determine existence of a vertex with a null id");
        return graph.getVertices(EntityType.ID_KEY, id).iterator().hasNext();
    }

    @Override
    public <E> E getEntity(String id, Class<E> cls) throws ItemNotFound {
        return graph.frame(getVertex(id), cls);
    }

    @Override
    public <E> E getEntityUnchecked(String id, Class<E> cls) {
        try {
            return graph.frame(getVertex(id), cls);
        } catch (ItemNotFound itemNotFound) {
            return null;
        }
    }

    @Override
    public <E> E getEntity(String id, EntityClass type, Class<E> cls) throws ItemNotFound {
        Vertex vertex = getVertex(id);
        if (!getType(vertex).equals(type.getName())) {
            throw new ItemNotFound(id, getEntityClass(vertex));
        }
        return graph.frame(vertex, cls);
    }

    @Override
    public <E> CloseableIterable<E> getEntities(EntityClass type, Class<E> cls) {
        return new WrappingCloseableIterable<>(
                graph.frameVertices(getVertices(type), cls));
    }

    @Override
    public <E> CloseableIterable<E> getEntities(String key, Object value, EntityClass type, Class<E> cls) {
        return new WrappingCloseableIterable<>(
                graph.frameVertices(getVertices(key, value, type), cls));
    }

    @Override
    public Vertex getVertex(String id) throws ItemNotFound {
        Preconditions.checkNotNull(id, "attempt to fetch vertex with a null id");
        try {
            return graph.getVertices(EntityType.ID_KEY, id).iterator().next();
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        }
    }

    @Override
    public CloseableIterable<Vertex> getVertices(EntityClass type) {
        return new WrappingCloseableIterable<>(
                graph.getVertices(EntityType.TYPE_KEY, type.getName()));
    }

    @Override
    public CloseableIterable<Vertex> getVertices(Iterable<String> ids) {
        Iterable<Vertex> verts = Iterables.transform(ids, id -> {
            try {
                return getVertex(id);
            } catch (ItemNotFound e) {
                return null;
            }
        });
        return new WrappingCloseableIterable<>(verts);
    }

    @Override
    public CloseableIterable<Vertex> getVertices(String key, Object value, EntityClass type) {
        // NB: This is rather annoying.
        List<Vertex> elems = Lists.newArrayList();
        for (Vertex v : graph.getVertices(key, value)) {
            if (getEntityClass(v).equals(type)) {
                elems.add(v);
            }
        }
        return new WrappingCloseableIterable<>(elems);
    }

    @Override
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data) throws IntegrityError {
        Preconditions
                .checkNotNull(id, "null vertex ID given for item creation");
        Map<String, ?> indexData = getVertexData(id, type, data);
        checkExists(id);
        Vertex node = graph.addVertex(null);
        for (Map.Entry<String, ?> entry : indexData.entrySet()) {
            if (entry.getValue() == null)
                continue;
            node.setProperty(entry.getKey(), entry.getValue());
        }
        return node;
    }

    @Override
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data) throws ItemNotFound {
        Preconditions.checkNotNull(id, "null vertex ID given for item update");
        Map<String, ?> indexData = getVertexData(id, type, data);
        try {
            Vertex node = getVertex(id);
            replaceProperties(node, indexData);
            return node;
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
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
        if (value == null) {
            vertex.removeProperty(key);
        } else {
            vertex.setProperty(key, value);
        }
    }

    @Override
    public void renameVertex(Vertex vertex, String oldId, String newId) {
        Preconditions.checkNotNull(vertex);
        Preconditions.checkNotNull(newId);
        vertex.setProperty(EntityType.ID_KEY, newId);
    }

    @Override
    public void deleteVertex(String id) throws ItemNotFound {
        deleteVertex(getVertex(id));
    }

    @Override
    public void deleteVertex(Vertex vertex) {
        vertex.remove();
    }

    @Override
    public void initialize() {
    }

    private <E extends Element> void replaceProperties(E item, Map<String, ?> data) {
        // remove 'old' properties
        for (String key : item.getPropertyKeys()) {
            if (!key.startsWith(METADATA_PREFIX)) {
                item.removeProperty(key);
            }
        }

        // add all 'new' properties to the relationship and index
        addProperties(item, data);
    }

    private <E extends Element> void addProperties(E item, Map<String, ?> data) {
        Preconditions.checkNotNull(data, "Data map cannot be null");
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            item.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private void checkExists(String id) throws IntegrityError {
        if (exists(id)) {
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
}
