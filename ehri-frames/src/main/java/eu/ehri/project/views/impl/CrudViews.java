package eu.ehri.project.views.impl;

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
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
     * @param graph The graph
     * @param cls   The entity class to return
     * @param scope The permission scope
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
     * @param graph The graph
     * @param cls   The entity class to return
     */
    public CrudViews(FramedGraph<?> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Fetch an item, as a user. This only provides access control.
     *
     * @param item The item
     * @param user The current user
     * @return The given framed vertex
     * @throws AccessDenied
     */
    public E detail(E item, Accessor user) throws AccessDenied {
        helper.checkReadAccess(item, user);
        return item;
    }

    /**
     * Update an object bundle, also updating dependent items.
     *
     * @param bundle     The item's data bundle
     * @param user       The current user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    public Mutation<E> update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, IntegrityError, ItemNotFound {
        E entity = graph.frame(manager.getVertex(bundle.getId()), cls);
        helper.checkEntityPermission(entity, user, PermissionType.UPDATE);
        return getPersister(scope).update(bundle, cls);
    }

    /**
     * Update an object bundle representing a dependent item, also updating *it's*
     * dependent items.
     *
     * @param bundle The item's data
     * @param parent The item's parent node
     * @param user The current user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    public <T extends Frame, P extends DescribedEntity> Mutation<T> updateDependent(Bundle bundle, P parent,
            Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, IntegrityError, ItemNotFound {
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        return getPersister(parent).update(bundle, dependentClass);
    }

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     *
     * @param bundle     The item's data bundle
     * @param user       The current user
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
        E item = getPersister(scope).create(bundle, cls);
        // If a user creates an item, grant them OWNER perms on it.
        if (!acl.belongsToAdmin(user))
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
     * @param bundle The item's data
     * @param parent The item's parent node
     * @param user The current user
     * @param dependentCls The dependent class
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public <T extends Frame, P extends DescribedEntity> T createDependent(Bundle bundle, P parent,
            Accessor user, Class<T> dependentCls)
            throws PermissionDenied, ValidationError,
            IntegrityError {
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        return getPersister(parent).create(bundle, dependentCls);
    }

    /**
     * Create or update a new object of type `E` from the given data, within the
     * scope of `scope`.
     *
     * @param bundle     The item's data bundle
     * @param user       The current user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     */
    public Mutation<E> createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, IntegrityError {
        helper.checkContentPermission(user, helper.getContentType(cls),
                PermissionType.CREATE);
        helper.checkContentPermission(user, helper.getContentType(cls),
                PermissionType.UPDATE);
        return getPersister(scope).createOrUpdate(bundle, cls);
    }

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param item The item
     * @param user The current user
     * @return The number of vertices deleted.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError {
        helper.checkEntityPermission(item, user, PermissionType.DELETE);
        return getPersister(scope).delete(serializer
                .vertexFrameToBundle(item));
    }

    /**
     * Delete a dependent item bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param item The item
     * @param user The current user
     * @return The number of vertices deleted.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public <T extends Frame, P extends DescribedEntity> Integer deleteDependent(T item, P parent,
                Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, SerializationError {
        helper.checkEntityPermission(parent, user, PermissionType.DELETE);
        return getPersister(parent).delete(serializer
                .vertexFrameToBundle(item));
    }

    /**
     * Set the permission scope of the view.
     *
     * @param scope A permission scope
     * @return A new view
     */
    public Crud<E> setScope(PermissionScope scope) {
        return new CrudViews<E>(graph, cls,
                Optional.fromNullable(scope).or(SystemScope.INSTANCE));
    }

    // Helpers
    private BundleDAO getPersister(PermissionScope scope) {
        return new BundleDAO(graph, scope.idPath());
    }
}
