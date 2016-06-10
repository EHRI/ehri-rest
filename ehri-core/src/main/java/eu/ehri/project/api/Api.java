package eu.ehri.project.api;

import com.google.common.base.Optional;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import eu.ehri.project.acl.InheritedItemPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.api.impl.ApiImpl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Api {


    Accessor accessor();

    Api withAccessor(Accessor accessor);

    Api withScope(PermissionScope permissionScope);

    Api enableLogging(boolean logEnabled);

    ActionManager actionManager();

    VirtualUnitsApi virtualUnits();

    UserProfilesApi userProfiles();

    ConceptsApi concepts();

    Acl acl();

    AclManager aclManager();

    EventsApi events();

    QueryApi query();

    /**
     * Fetch an item, as a user. This only provides access control.
     *
     * @param id  the item id
     * @param cls the item's class
     * @return the given framed vertex
     * @throws ItemNotFound
     */
    <E extends Accessible> E detail(String id, Class<E> cls) throws ItemNotFound;

    /**
     * Update an object bundle, also updating dependent items.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @return the updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */
    <E extends Accessible> Mutation<E> update(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError;

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @return the created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <E extends Accessible> E create(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Create or update a new object of type `E` from the given data, within the
     * scope of `scope`.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @return the created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param id the item ID
     * @return the number of vertices deleted.
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    int delete(String id) throws PermissionDenied,
            ValidationError, SerializationError, ItemNotFound;

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the given log message.
     *
     * @param bundle     the item's data bundle
     * @param cls        the item's class
     * @param logMessage a log message
     * @return the created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <E extends Accessible> Mutation<E> update(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError;

    /**
     * Create a new object of type `E` from the given data, saving an Action log
     * with the given log message.
     *
     * @param bundle     the item's data bundle
     * @param cls        the item's class
     * @param logMessage a log message
     * @return the created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <E extends Accessible> E create(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Create or update a new object of type `E` from the given data, within the
     * scope of `scope`.
     *
     * @param bundle     the item's data bundle
     * @param cls        the item's class
     * @param logMessage a log message
     * @return the created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param id         the item ID
     * @param logMessage a log message
     * @return the number of vertices deleted.
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    int delete(String id, Optional<String> logMessage) throws PermissionDenied,
            ValidationError, SerializationError, ItemNotFound;

    /**
     * Delete a dependent item, belonging to the given parent.
     *
     * @param parentId   the parent ID
     * @param id         the dependent item's ID
     * @param logMessage an optional log message
     * @return the number of vertices deleted
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws SerializationError
     */
    int deleteDependent(String parentId, String id, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError;

    /**
     * Create a dependent item, belonging to the given parent.
     *
     * @param parentId   the parent ID
     * @param data       the dependent item data
     * @param cls        the dependent item's class
     * @param logMessage an optional log message
     * @param <T>        the dependent item's generic class
     * @return the dependent item frame
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <T extends Accessible> T createDependent(String parentId, Bundle data,
            Class<T> cls, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError;

    /**
     * Update a dependent item, belonging to the given parent.
     *
     * @param parentId   the parent ID
     * @param data       the dependent item data
     * @param cls        the dependent item's class
     * @param logMessage an optional log message
     * @param <T>        the dependent item's generic class
     * @return the dependent item frame
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws ValidationError
     */
    <T extends Accessible> Mutation<T> updateDependent(String parentId, Bundle data,
            Class<T> cls, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError;

    /**
     * Create a link between two items.
     *
     * @param targetId1 the identifier of a Accessible target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle    the annotation itself
     * @return a new link
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws PermissionDenied
     */
    Link createLink(String targetId1, String targetId2, List<String> bodies, Bundle bundle,
            Collection<Accessor> accessibleTo) throws ItemNotFound, ValidationError,
            PermissionDenied;

    /**
     * Create a link between two items, along with an access point on the given description.
     *
     * @param targetId1 the identifier of a Accessible target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle    the annotation itself
     * @return a new link
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws PermissionDenied
     */
    Link createAccessPointLink(String targetId1, String targetId2, String descriptionId, String bodyName,
            String bodyType, Bundle bundle, Collection<Accessor> accessibleTo)
            throws ItemNotFound, ValidationError, PermissionDenied;

    /**
     * Create an annotation for a dependent node of an item. The entity and the
     * dependent item can be the same.
     *
     * @param id           the identifier of the Accessible this annotation is attached to, as a target
     * @param did          the identifier of the dependent item
     * @param bundle       the annotation itself
     * @param accessibleTo users or groups who can access this annotation
     * @return the created annotation
     * @throws PermissionDenied
     * @throws AccessDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */

    Annotation createAnnotation(String id, String did, Bundle bundle, Collection<Accessor> accessibleTo)
            throws PermissionDenied, AccessDenied, ValidationError, ItemNotFound;

    /**
     * Up vote an item, removing a down vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws NotPromotableError
     */
    Promotable promote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove an up vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound
     * @throws PermissionDenied
     */
    Promotable removePromotion(String id) throws ItemNotFound, PermissionDenied;

    /**
     * Down vote an item, removing an up vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws NotPromotableError
     */
    Promotable demote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove a down vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound
     * @throws PermissionDenied
     */
    Promotable removeDemotion(String id) throws ItemNotFound, PermissionDenied;

    /**
     * Determine if logging is enabled.
     *
     * @return logging?
     */
    boolean isLogging();

    /**
     * ACL and permission related operations.
     */
    interface Acl {
        /**
         * Set the global permission matrix for a user.
         *
         * @param userOrGroup   the user or group
         * @param permissionSet the new permissions
         * @throws PermissionDenied
         */
        InheritedGlobalPermissionSet setGlobalPermissionMatrix(Accessor userOrGroup,
                GlobalPermissionSet permissionSet) throws PermissionDenied;


        void setAccessors(Accessible entity, Set<Accessor> accessors) throws PermissionDenied;

        /**
         * Set permissions for the given user on the given item.
         *
         * @param item           the item
         * @param accessor       the accessor
         * @param permissionList the set of permissions to grant
         * @return the new inherited item permission set
         * @throws PermissionDenied
         */
        InheritedItemPermissionSet setItemPermissions(Accessible item, Accessor accessor,
                Set<PermissionType> permissionList)
                throws PermissionDenied;

        /**
         * Revoke a permission grant.
         *
         * @param grant the permission grant
         * @throws PermissionDenied
         */
        void revokePermissionGrant(PermissionGrant grant)
                throws PermissionDenied;

        /**
         * Add a user to a group. This confers any permissions the group
         * has on the user, so requires grant permissions for the user as
         * we as modify permissions for the group.
         *
         * @param group       the group
         * @param userOrGroup the user to add to the group
         * @throws PermissionDenied
         */
        void addAccessorToGroup(Group group, Accessor userOrGroup) throws PermissionDenied;

        /**
         * Remove a user from a group. Just as with adding uers, this requires
         * grant permissions for the user and modify permissions for the group.
         *
         * @param group       the group
         * @param userOrGroup the user to add to the group
         * @throws PermissionDenied
         */
        void removeAccessorFromGroup(Group group, Accessor userOrGroup) throws PermissionDenied;
    }

    class NotPromotableError extends Exception {
        public NotPromotableError(String itemId) {
            super(String.format("Item '%s' is not marked as promotable.", itemId));
        }
    }
}
