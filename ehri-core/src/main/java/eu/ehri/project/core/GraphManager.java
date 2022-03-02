/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.core;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Entity;

import java.util.Map;

/**
 * An abstraction over the basic {@link FramedGraph} class
 * that provides unified CRUD semantics for low-level
 * graph operations.
 */
public interface GraphManager {

    /**
     * Get a pointer to the underlying graph.
     *
     * @return the underlying graph object
     */
    FramedGraph<?> getGraph();

    /**
     * Get the id of a given vertex.
     *
     * @param vertex A vertex
     * @return The vertex's string ID
     */
    String getId(Vertex vertex);

    /**
     * Get the id of a given vertex.
     *
     * @param vertex A vertex
     * @return The vertex's string type
     */
    String getType(Vertex vertex);

    /**
     * Get a vertex's properties.
     *
     * @param vertex A vertex
     * @return a map of property objects
     */
    Map<String, Object> getProperties(Vertex vertex);

    /**
     * Get the type of an arbitrary vertex.
     *
     * @param vertex A vertex
     * @return The vertex's entity class
     */
    EntityClass getEntityClass(Vertex vertex);

    /**
     * Get the type of an arbitrary framed vertex.
     *
     * @param entity A framed vertex
     * @return The entity's entity class
     */
    EntityClass getEntityClass(Entity entity);

    /**
     * Check if a node with the given ID exists or not.
     *
     * @param id A string id
     * @return Whether or not a node with that ID exists in the graph.
     */
    boolean exists(String id);

    /**
     * Get a node with the given ID.
     *
     * @param id The vertex's string ID
     * @return The vertex
     * @throws ItemNotFound If the given vertex does not exist
     */
    Vertex getVertex(String id) throws ItemNotFound;

    /**
     * Get a node with the given ID, and frame it with the given interface
     * class.
     *
     * @param id  The vertex's string ID
     * @param cls The desired frame class
     * @param <T> the expected type of the entity
     * @return The framed vertex
     * @throws ItemNotFound If the given vertex does not exist
     */
    <T> T getEntity(String id, Class<T> cls) throws ItemNotFound;

    /**
     * Get a node with the given ID, and frame it with the given interface
     * class, returning null if it does not exist.
     *
     * @param id  The vertex's string ID
     * @param cls The desired frame class
     * @param <E> the expected type of the entity
     * @return The framed vertex, or null
     */
    <E> E getEntityUnchecked(String id, Class<E> cls);

    /**
     * Get a node with the given ID and type, framing it with the given
     * interface class.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param cls  The desired frame class
     * @param <T>  The generic type of the returned items
     * @return The framed vertex
     * @throws ItemNotFound If the given vertex does not exist
     */
    <T> T getEntity(String id, EntityClass type, Class<T> cls) throws ItemNotFound;

    /**
     * Get a CloseableIterable of vertices with the given entity class.
     *
     * @param type The entity type
     * @return An iterable of vertices belonging to that entity class
     */
    CloseableIterable<Vertex> getVertices(EntityClass type);

    /**
     * Get a CloseableIterable of vertices with the given ids.
     *
     * @param ids An iterable of String IDs
     * @return An iterable of vertices with the given IDs. If a vertex
     * is not found the value at the place in the input iterable will
     * be null.
     */
    CloseableIterable<Vertex> getVertices(Iterable<String> ids);

    /**
     * Get a CloseableIterable of vertices with the given type, and the given
     * key/value property.
     *
     * @param key   the property key
     * @param value property value
     * @param type  the entity type
     * @return an iterable of vertices with the given key/value properties
     */
    CloseableIterable<Vertex> getVertices(String key, Object value, EntityClass type);

    /**
     * Get an Iterable of vertices of the given type, frames with the given
     * interface class.
     *
     * @param type The entity type
     * @param cls  the class of the returned items
     * @param <T>  the generic type of the returned items
     * @return An iterable of framed vertices with the given framed class.
     */
    <T> CloseableIterable<T> getEntities(EntityClass type, Class<T> cls);

    /**
     * Get a CloseableIterable of entities with the given type, and the given
     * key/value property.
     *
     * @param key   the property key
     * @param value property value
     * @param type  the entity type
     * @param cls   the class of the returned items
     * @param <T>   the generic type of the returned items
     * @return an iterable of entities with the given key/value properties
     */
    <T> CloseableIterable<T> getEntities(String key, Object value, EntityClass type, Class<T> cls);

    // CRUD functions

    /**
     * Create a vertex with the given id, type, and data.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @return The new vertex
     * @throws IntegrityError if an item with the given id already exists
     */
    Vertex createVertex(String id, EntityClass type, Map<String, ?> data) throws IntegrityError;

    /**
     * Create a vertex with the given id, type, and data.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @return The updated vertex
     * @throws ItemNotFound If the given vertex does not exist
     */
    Vertex updateVertex(String id, EntityClass type, Map<String, ?> data) throws ItemNotFound;

    /**
     * Set a property on a vertex.
     *
     * @param vertex The vertex
     * @param key    The property key
     * @param value  The property value
     */
    void setProperty(Vertex vertex, String key, Object value);

    /**
     * Rename an existing vertex, changing its ID.
     *
     * @param vertex the vertex
     * @param oldId  the old ID
     * @param newId  the new ID
     */
    void renameVertex(Vertex vertex, String oldId, String newId);

    // CRUD functions

    /**
     * Delete a vertex with the given ID.
     *
     * @param id The vertex's string ID
     * @throws ItemNotFound If the given vertex does not exist
     */
    void deleteVertex(String id) throws ItemNotFound;

    /**
     * Delete the given vertex.
     *
     * @param vertex The vertex to delete
     */
    void deleteVertex(Vertex vertex);

    /*
     * Run graph-specific initialization code.
     */
    void initialize();
}
