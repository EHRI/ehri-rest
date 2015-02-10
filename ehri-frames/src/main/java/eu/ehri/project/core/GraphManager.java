package eu.ehri.project.core;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Frame;

import java.util.Map;

/**
 * An abstraction over the basic {@link FramedGraph} class
 * that provides unified CRUD semantics for low-level
 * graph operations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface GraphManager {

    /**
     * Get a pointer to the underlying graph.
     */
    public FramedGraph<?> getGraph();

    /**
     * Cast one frame to another.
     *
     * @param frame The given frame
     * @param cls   The frame class to cast into
     * @return The input frame cast as the output class
     */
    public <T extends Frame> T cast(Frame frame, Class<T> cls);

    /**
     * Get the id of a given vertex.
     *
     * @param vertex A vertex
     * @return The vertex's string ID
     */
    public String getId(Vertex vertex);

    /**
     * Get the id of a given vertex.
     *
     * @param vertex A vertex
     * @return The vertex's string type
     */
    public String getType(Vertex vertex);

    /**
     * Get the type of an arbitrary vertex.
     *
     * @param vertex A vertex
     * @return The vertex's entity class
     */
    public EntityClass getEntityClass(Vertex vertex);

    /**
     * Get the type of an arbitrary framed vertex.
     *
     * @param frame A framed vertex
     * @return The frame's entity class
     */
    public EntityClass getEntityClass(Frame frame);

    /**
     * Check if a node with the given ID exists or not.
     *
     * @param id A string id
     * @return Whether or not a node with that ID exists in the graph.
     */
    public boolean exists(String id);

    /**
     * Get a node with the given ID.
     *
     * @param id The vertex's string ID
     * @return The vertex
     */
    public Vertex getVertex(String id) throws ItemNotFound;

    /**
     * Get a node with the given ID and type.
     *
     * @param id The vertex's string ID
     * @return The vertex
     */
    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound;

    /**
     * Get a node with the given ID, and frame it with the given interface
     * class.
     *
     * @param id  The vertex's string ID
     * @param cls The desired frame class
     * @return The framed vertex
     */
    public <T> T getFrame(String id, Class<T> cls) throws ItemNotFound;

    /**
     * Get a node with the given ID and type, framing it with the given
     * interface class.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param cls  The desired frame class
     * @return The framed vertex
     */
    public <T> T getFrame(String id, EntityClass type, Class<T> cls)
            throws ItemNotFound;

    /**
     * Get a CloseableIterable of vertices with the given entity class.
     *
     * @param type The entity type
     * @return An iterable of vertices belonging to that entity class
     */
    public CloseableIterable<Vertex> getVertices(EntityClass type);

    /**
     * Get a CloseableIterable of vertices with the given ids.
     *
     * @param ids An iterable of String IDs
     * @return An iterable of vertices with the given IDs
     */
    public CloseableIterable<Vertex> getVertices(Iterable<String> ids) throws ItemNotFound;

    /**
     * Get a CloseableIterable of vertices with the given type, and the given
     * key/value indexed property.
     *
     * @param type The entity type
     * @return An iterable of vertices with the given key/value properties
     */
    public CloseableIterable<Vertex> getVertices(String key,
            Object value, EntityClass type);

    /**
     * Get an Iterable of vertices of the given type, frames with the given
     * interface class.
     *
     * @param type The entity type
     * @return An iterable of framed vertices with the given framed class.
     */
    public <T> CloseableIterable<T> getFrames(EntityClass type, Class<T> cls);

    // CRUD functions

    /**
     * Create a vertex with the given id, type, and data.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @return The new vertex
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data) throws IntegrityError;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @param keys A set of keys to be indexed
     * @return The new vertex
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys)
            throws IntegrityError;

    /**
     * Create a vertex with the given id, type, and data.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @return The updated vertex
     * @throws ItemNotFound
     */
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data) throws ItemNotFound;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed.
     *
     * @param id   The vertex's string ID
     * @param type The entity type
     * @param data The data map
     * @param keys A set of keys to be indexed
     * @return The updated vertex
     * @throws ItemNotFound
     */
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws ItemNotFound;

    /**
     * Set a property on a vertex.
     *
     * @param vertex The vertex
     * @param key    The property key
     * @param value  The property value
     */
    public void setProperty(Vertex vertex, String key, Object value);

    /**
     * Rename an existing vertex, changing its ID.
     *
     * @param vertex the vertex
     * @param vertex the old ID
     * @param newId  the new ID
     */
    public void renameVertex(Vertex vertex, String oldId, String newId);

    // CRUD functions

    /**
     * Delete a vertex with the given ID.
     *
     * @param id The vertex's string ID
     */
    public void deleteVertex(String id) throws ItemNotFound;

    /**
     * Delete the given vertex.
     *
     * @param vertex The vertex to delete
     */
    public void deleteVertex(Vertex vertex);

    /**
     * Rebuild the internal graph index.
     */
    public void rebuildIndex();
}
