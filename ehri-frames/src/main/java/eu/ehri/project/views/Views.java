package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.EntityBundle;

public class Views<E extends AccessibleEntity> extends AbstractViews<E>
        implements IViews<E> {

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
    public E detail(E entity, Accessor user) throws PermissionDenied {
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
     * @throws IntegrityError
     */
    public E update(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        EntityBundle<E> bundle = converter.dataToBundle(data);
        E entity = graph.getVertex(bundle.getId(), cls);
        checkEntityPermission(entity, user, getPermission(PermissionTypes.UPDATE));
        return new BundleDAO<E>(graph).update(bundle);
    }

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     * 
     * @param data
     * @param user
     * @return
     * @throws DeserializationError
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public E create(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        checkPermission(user, getPermission(PermissionTypes.CREATE));
        EntityBundle<E> bundle = converter.dataToBundle(data);
        return new BundleDAO<E>(graph).create(bundle);
    }

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     * 
     * @param item
     * @param user
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError {
        checkEntityPermission(item, user, getPermission(PermissionTypes.DELETE));
        return new BundleDAO<E>(graph).delete(converter
                .vertexFrameToBundle(item));
    }
}
