package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.relationships.Access;
import eu.ehri.project.acl.AclManager;

public class Views<E extends AccessibleEntity> implements IViews<E> {

    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;

    /**
     * @param graph
     * @param cls
     */
    public Views(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }

    /**
     * Ensure an item is readable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkReadAccess(AccessibleEntity entity, Long user)
            throws PermissionDenied {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = acl.getAccessControl(entity, accessor);
        if (!access.getRead())
            throw new PermissionDenied(accessor, entity);
    }

    /**
     * Ensure an item is writable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, Long user)
            throws PermissionDenied {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = acl.getAccessControl(entity, accessor);
        if (!(access.getRead() && access.getWrite()))
            throw new PermissionDenied(accessor, entity);
    }

    protected void checkGlobalWriteAccess(Long user) throws PermissionDenied {
        // TODO: Stub
    }

    /**
     * Return a string representation of the given item.
     * 
     * @param item
     * @param user
     * @return
     * @throws PermissionDenied
     */
    public E detail(Long item, Long user) throws PermissionDenied {
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
    public E update(Map<String, Object> data, Long user)
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
    public E create(Map<String, Object> data, Long user)
            throws PermissionDenied, ValidationError, DeserializationError {
        checkGlobalWriteAccess(user);
        EntityBundle<E> bundle = converter.dataToBundle(data);
        return new BundleDAO<E>(graph).insert(bundle);
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
    public Integer delete(Long item, Long user) throws PermissionDenied,
            ValidationError, SerializationError {
        E entity = graph.getVertex(item, cls);
        checkGlobalWriteAccess(user);
        checkWriteAccess(entity, user);
        return new BundleDAO<E>(graph).delete(converter
                .vertexFrameToBundle(entity));
    }
}
