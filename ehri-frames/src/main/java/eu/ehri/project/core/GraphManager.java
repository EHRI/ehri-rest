package eu.ehri.project.core;

import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;

public interface GraphManager {

    /**
     * Get the id of a given vertex.
     */
    public String getId(Vertex vertex);

    /**
     * Get the id of a given vertex frame.
     */
    public String getId(VertexFrame vertex);
    
    /**
     * Get the type of an arbitrary vertex.
     */
    public EntityClass getType(Vertex vertex);

    /**
     * Get the type of an arbitrary vertex.
     */
    public EntityClass getType(VertexFrame vertex);
    
    /**
     * Check if a node with the given ID exists or not.
     */
    public boolean exists(String id);

    /**
     * Get a node with the given ID.
     * 
     * @param id
     * @return
     */
    public Vertex getVertex(String id) throws ItemNotFound;

    /**
     * Get a node with the given ID and type.
     * 
     * @param id
     * @return
     */
    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound;

    /**
     * Get a node with the given ID, and frame it with the given interface
     * class.
     * 
     * @param id
     * @param cls
     * @return
     */
    public <T> T getFrame(String id, Class<T> cls) throws ItemNotFound;

    /**
     * Get a node with the given ID and type, framing it with the given
     * interface class.
     * 
     * @param id
     * @param type
     * @param cls
     * @return
     */
    public <T> T getFrame(String id, EntityClass type, Class<T> cls) throws ItemNotFound;

    /**
     * Get a CloseableIterable of vertices with the given type.
     * 
     * @param type
     * @return
     */
    public CloseableIterable<Vertex> getVertices(EntityClass type);

    /**
     * Get a CloseableIterable of vertices with the given type, and the given
     * key/value indexed property.
     * 
     * @param type
     * @return
     */
    public <T extends Vertex> CloseableIterable<T> getVertices(String key,
            Object value, EntityClass type);

    /**
     * Get an Iterable of vertices of the given type, frames with the given
     * interface class.
     * 
     * @param type
     * @return
     */
    public <T> Iterable<T> getFrames(EntityClass type, Class<T> cls);

    /**
     * List the given property for objects of a given type.
     * 
     * @param type
     * @param propertyName
     * @return
     */
    public List<Object> getAllPropertiesOfType(EntityClass type, String propertyName);

    // CRUD functions

    /**
     * Delete a vertex with the given ID.
     * 
     * @param id
     */
    public void deleteVertex(String id) throws ItemNotFound;

    /**
     * Delete the given vertex.
     * 
     * @param vertex
     */
    public void deleteVertex(Vertex vertex);

    /**
     * Create a vertex with the given id, type, and data.
     * 
     * @param id
     * @param type
     * @param data
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, EntityClass type, Map<String, Object> data)
            throws IntegrityError;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed.
     * 
     * @param id
     * @param type
     * @param data
     * @param keys
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys) throws IntegrityError;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed, and which should be unique.
     * 
     * @param id
     * @param type
     * @param data
     * @param keys
     * @param uniqueKeys
     * @return
     * @throws IntegrityError
     */
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys,
            Iterable<String> uniqueKeys) throws IntegrityError, ItemNotFound;

    /**
     * Create a vertex with the given id, type, and data.
     * 
     * @param id
     * @param type
     * @param data
     * @return
     * @throws IntegrityError
     */
    public Vertex updateVertex(String id, EntityClass type, Map<String, Object> data)
            throws IntegrityError, ItemNotFound;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed.
     * 
     * @param id
     * @param type
     * @param data
     * @param keys
     * @return
     * @throws IntegrityError
     */
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys)
            throws IntegrityError, ItemNotFound;

    /**
     * Create a vertex with the given id, type, and data, specifying which
     * property keys should be indexed, and which should be unique.
     * 
     * @param id
     * @param type
     * @param data
     * @param keys
     * @param uniqueKeys
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys,
            Iterable<String> uniqueKeys) throws IntegrityError;
}
