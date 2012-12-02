package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;

public final class Views<E extends AccessibleEntity> implements IViews<E> {
    private final FramedGraph<Neo4jGraph> graph;
    private final Class<E> cls;
    private final ViewHelper helper;
    private final GraphManager manager;
    private final Converter converter;
    private final PermissionScope scope;

    /**
     * Scoped Constructor.
     * 
     * @param graph
     * @param cls
     */
    public Views(FramedGraph<Neo4jGraph> graph, Class<E> cls, PermissionScope scope) {
        this.graph = graph;
        this.cls = cls;
        this.scope = scope;
        helper = new ViewHelper(graph, cls, scope);
        converter = new Converter();
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public Views(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Return a string representation of the given item.
     * 
     * @param item
     * @param user
     * @return The given framed vertex
     * @throws PermissionDenied
     */
    public E detail(E entity, Accessor user) throws PermissionDenied {
        helper.checkReadAccess(entity, user);
        return entity;
    }

    /**
     * Update an object bundle, also updating dependent items.
     * 
     * @param data
     * @param user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public E update(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        EntityBundle<E> bundle = converter.dataToBundle(data);
        E entity = graph.frame(manager.getVertex(bundle.getId()), cls);
        helper.checkEntityPermission(entity, user,
                helper.getPermission(PermissionTypes.UPDATE));
        return new BundleDAO<E>(graph, scope).update(bundle);
    }

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     * 
     * @param data
     * @param user
     * @return The created framed vertex
     * @throws DeserializationError
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public E create(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        helper.checkPermission(user, helper.getPermission(PermissionTypes.CREATE));
        EntityBundle<E> bundle = converter.dataToBundle(data);
        return new BundleDAO<E>(graph, scope).create(bundle);
    }

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     * 
     * @param item
     * @param user
     * @return The number of vertices deleted.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError {
        helper.checkEntityPermission(item, user, helper.getPermission(PermissionTypes.DELETE));
        return new BundleDAO<E>(graph, scope).delete(converter
                .vertexFrameToBundle(item));
    }

    public IViews<E> setScope(PermissionScope scope) {
        return new Views<E>(graph, cls, scope);
    }
}
