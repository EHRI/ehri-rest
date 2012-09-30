package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.EntityBundle;

public class Views<E extends AccessibleEntity> extends AbstractViews<E> implements IViews<E> {

    
    
    public Views(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        super(graph, cls);
    }

    /**
     * Return a string representation of the given item.
     * 
     * @param item
     * @param user
     * @return
     * @throws PermissionDenied
     */
    public E detail(long item, long user) throws PermissionDenied {
        E entity = graph.getVertex(item, cls);
        checkReadAccess(entity, user);
        return entity;
    }

    /**
     * Update an object bundle, also updating dependent items.
     * 
     * @param data
     * @param user
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     */
    public E update(Map<String, Object> data, long user)
            throws PermissionDenied, ValidationError, DeserializationError {
        EntityBundle<E> bundle = converter.dataToBundle(data);
        E entity = graph.getVertex(bundle.getId(), cls);
        checkWriteAccess(entity, user);
        return new BundleDAO<E>(graph).update(bundle);
    }

    /**
     * Create a new object of type `E` from the given data.
     * 
     * @param data
     * @param user
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     */
    public E create(Map<String, Object> data, long user)
            throws PermissionDenied, ValidationError, DeserializationError {
        checkGlobalWriteAccess(user);
        EntityBundle<E> bundle = converter.dataToBundle(data);
        return new BundleDAO<E>(graph).create(bundle);
    }

    /**
     * Delete an object bundle, following dependency cascades.
     * 
     * @param item
     * @param user
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(long item, long user) throws PermissionDenied,
            ValidationError, SerializationError {
        E entity = graph.getVertex(item, cls);
        checkGlobalWriteAccess(user);
        checkWriteAccess(entity, user);
        return new BundleDAO<E>(graph).delete(converter
                .vertexFrameToBundle(entity));
    }
}
