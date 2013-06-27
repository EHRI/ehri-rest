package eu.ehri.project.views.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewHelper;

public final class CrudViews<E extends AccessibleEntity> implements Crud<E> {
    private final FramedGraph<?> graph;
    private final Class<E> cls;
    private final ViewHelper helper;
    private final GraphManager manager;
    private final Serializer serializer;
    private final PermissionScope scope;
    private final AclManager acl;

    /**
     * Scoped Constructor.
     *
     * @param graph
     * @param cls
     */
    public CrudViews(FramedGraph<?> graph, Class<E> cls,
            PermissionScope scope) {
        this.graph = graph;
        this.cls = cls;
        this.scope = Optional.fromNullable(scope).or(SystemScope.getInstance());
        helper = new ViewHelper(graph, this.scope);
        acl = helper.getAclManager();
        serializer = new Serializer(graph);
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor.
     *
     * @param graph
     * @param cls
     */
    public CrudViews(FramedGraph<?> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Return a string representation of the given item.
     *
     * @param entity
     * @param user
     * @return The given framed vertex
     * @throws AccessDenied
     */
    public E detail(E entity, Accessor user) throws AccessDenied {
        helper.checkReadAccess(entity, user);
        return entity;
    }

    /**
     * Update an object bundle, also updating dependent items.
     *
     * @param bundle
     * @param user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    public E update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, IntegrityError, ItemNotFound {
        E entity = graph.frame(manager.getVertex(bundle.getId()), cls);
        helper.checkEntityPermission(entity, user, PermissionType.UPDATE);
        return new BundleDAO(graph, scope).update(bundle, cls);
    }

    /**
     * Update an object bundle representing a dependent item, also updating *it's*
     * dependent items.
     *
     * @param bundle
     * @param parent
     * @param user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    public <T extends Frame> T updateDependent(Bundle bundle, E parent,
            Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, IntegrityError, ItemNotFound {
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        return new BundleDAO(graph, scope).update(bundle, dependentClass);
    }

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     *
     * @param bundle
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public E create(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError,
            IntegrityError {
        helper.checkContentPermission(user, helper.getContentType(cls),
                PermissionType.CREATE);
        E item = new BundleDAO(graph, scope).create(bundle, cls);
        // If a user creates an item, grant them OWNER perms on it.
        if (!acl.isAdmin(user))
            acl.grantPermissions(user, item, PermissionType.OWNER);
        // If the scope is not the system, set the permission scope
        // of the item too...
        if (!scope.equals(SystemScope.getInstance())) {
            item.setPermissionScope(scope);
        }
        return item;
    }

    /**
     * Create a dependent item of parent item E. The relationships
     * between the items is not automatically set and must subsequently
     * be established.
     *
     * @param bundle
     * @param parent
     * @param user
     * @param dependentCls
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public <T extends Frame> T createDependent(Bundle bundle, E parent,
            Accessor user, Class<T> dependentCls)
            throws PermissionDenied, ValidationError,
            IntegrityError {
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        return new BundleDAO(graph, scope).create(bundle, dependentCls);
    }

    /**
     * Create or update a new object of type `E` from the given data, within the
     * scope of `scope`.
     *
     * @param bundle
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public E createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, IntegrityError {
        helper.checkContentPermission(user, helper.getContentType(cls),
                PermissionType.CREATE);
        helper.checkContentPermission(user, helper.getContentType(cls),
                PermissionType.UPDATE);
        return new BundleDAO(graph, scope).createOrUpdate(bundle, cls);
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
        helper.checkEntityPermission(item, user, PermissionType.DELETE);
        return new BundleDAO(graph, scope).delete(serializer
                .vertexFrameToBundle(item));
    }

    /**
     * Delete a dependent item bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param item
     * @param user
     * @return The number of vertices deleted.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public <T extends Frame> Integer deleteDependent(T item, E parent,
                Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, SerializationError {
        helper.checkEntityPermission(parent, user, PermissionType.DELETE);
        return new BundleDAO(graph, scope).delete(serializer
                .vertexFrameToBundle(item));
    }

    public Crud<E> setScope(PermissionScope scope) {
        return new CrudViews<E>(graph, cls,
                Optional.fromNullable(scope).or(SystemScope.INSTANCE));
    }
}
