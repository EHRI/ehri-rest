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

package eu.ehri.project.api;

import eu.ehri.project.acl.*;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.persistence.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    VersionManager versionManager();

    EventsApi events();

    QueryApi query();

    Serializer serializer();

    interface BatchCallback {
        void onItemDeleted(int itemNumber, String id);
    }

    /**
     * Fetch an item, as a user. This only provides access control.
     *
     * @param id  the item id
     * @param cls the item's class
     * @param <E> the type of entity
     * @return the given framed vertex
     * @throws ItemNotFound if the item does not exist
     */
    <E extends Accessible> E get(String id, Class<E> cls) throws ItemNotFound;

    /**
     * Update an object bundle, also updating dependent items.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @param <E>    the type of entity
     * @return the updated framed vertex
     * @throws ItemNotFound         if the item does not exist
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
     */
    <E extends Accessible> Mutation<E> update(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError;

    /**
     * Create a new object of type `E` from the given data, within the scope of
     * `scope`.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @param <E>    the type of entity
     * @return the created framed vertex
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
     */
    <E extends Accessible> E create(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Create or update a new object of type `E` from the given data, within the
     * scope of `scope`.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @param <E>    the type of entity
     * @return the created framed vertex
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
     */
    <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param id the item ID
     * @return the number of vertices deleted.
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws ValidationError    if the input data is incomplete or does not meet requirements
     * @throws HierarchyError     if the action would cause structural problems with an item hierarchy
     * @throws SerializationError if the data cannot be serialized
     */
    int delete(String id) throws PermissionDenied,
            ValidationError, SerializationError, ItemNotFound, HierarchyError;

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the given log message.
     *
     * @param bundle     the item's data bundle
     * @param cls        the item's class
     * @param logMessage a log message
     * @param <E>        the type of entity
     * @return the created framed vertex
     * @throws ItemNotFound         if the item does not exist
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
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
     * @param <E>        the type of entity
     * @return the created framed vertex
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
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
     * @param <E>        the type of entity
     * @return the created framed vertex
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if the input data is incomplete or does not meet requirements
     * @throws DeserializationError if the input data cannot be deserialized correctly
     */
    <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param id         the item ID
     * @param logMessage a log message
     * @return the number of vertices deleted
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws HierarchyError     if the action would cause structural problems with an item hierarchy
     * @throws SerializationError if a serialization error occurs during the operation
     */
    int delete(String id, Optional<String> logMessage) throws PermissionDenied,
            SerializationError, ItemNotFound, HierarchyError;

    /**
     * Delete a dependent item, belonging to the given parent.
     *
     * @param parentId   the parent ID
     * @param id         the dependent item's ID
     * @param logMessage an optional log message
     * @return the number of vertices deleted
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws SerializationError if a serialization error occurs during the operation
     */
    int deleteDependent(String parentId, String id, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError;

    /**
     * Delete all item's within the given item's permission scope.
     *
     * @param parentId   the parent ID
     * @param all        whether to recursively delete children of this item
     * @param version    whether to create versions of deleted items
     * @param logMessage an optional log message
     * @return the IDs of delete items
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws HierarchyError     if {{all}} is not given and the action would cause
     *                            structural problems with an item hierarchy
     * @throws SerializationError if a serialization error occurs during the operation
     */
    List<String> deleteChildren(String parentId, boolean all, boolean version, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError, HierarchyError;

    /**
     * Delete all item's within the given item's permission scope.
     *
     * @param parentId   the parent ID
     * @param all        whether to recursively delete children of this item
     * @param version    whether to create versions of deleted items
     * @param callback   a callback to run when an item is deleted
     * @param logMessage an optional log message
     * @return the IDs of delete items
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws HierarchyError     if {{all}} is not given and the action would cause
     *                            structural problems with an item hierarchy
     * @throws SerializationError if a serialization error occurs during the operation
     */
    List<String> deleteChildren(String parentId, boolean all, boolean version, BatchCallback callback, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError, HierarchyError;

    /**
     * Create a dependent item, belonging to the given parent.
     *
     * @param parentId   the parent ID
     * @param data       the dependent item data
     * @param cls        the dependent item's class
     * @param logMessage an optional log message
     * @param <T>        the dependent item's generic class
     * @return the dependent item frame
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if the input data is incomplete or does not meet requirements
     */
    <T extends Accessible> T createDependent(String parentId, Bundle data, Class<T> cls, Optional<String> logMessage)
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
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws ValidationError    if the input data is incomplete or does not meet requirements
     * @throws SerializationError if the data cannot be serialized
     */
    <T extends Accessible> Mutation<T> updateDependent(String parentId, Bundle data,
                                                       Class<T> cls, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError, SerializationError;

    /**
     * Get an item's links.
     *
     * Links are skipped if their targets are not accessible to the
     * current user.
     *
     * @param id the target item
     * @return an iterable of item links
     */
    Iterable<Link> getLinks(String id);

    /**
     * Create a link between two items.
     *
     * @param source       the identifier of a Accessible source of this Annotation
     * @param target       the identifier of a Annotator target of this Annotation
     * @param bundle       the annotation itself
     * @param bodies       the set of link body items
     * @param accessibleTo users who have exclusive access to this link
     * @param directional  whether the link is directional or not
     * @param logMessage   a log message
     * @return a new link
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if the input data is incomplete or does not meet requirements
     */
    Link createLink(String source,
                    String target,
                    List<String> bodies,
                    Bundle bundle,
                    boolean directional,
                    Collection<Accessor> accessibleTo,
                    Optional<String> logMessage) throws ItemNotFound, ValidationError,
            PermissionDenied;

    /**
     * Create a link between two items, along with an access point on the given description.
     *
     * @param source        the identifier of a Accessible target of this Annotation
     * @param target        the identifier of a Annotator source of this Annotation
     * @param bundle        the annotation itself
     * @param bodyName      the text of the created access point
     * @param bodyType      the type of the access point
     * @param accessibleTo  users who have exclusive access to this link
     * @param descriptionId the ID of the description to which the body will attach
     * @param logMessage    a log message
     * @return a new link
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if the input data is incomplete or does not meet requirements
     */
    Link createAccessPointLink(String source, String target, String descriptionId, String bodyName,
                               AccessPointType bodyType, Bundle bundle, Collection<Accessor> accessibleTo, Optional<String> logMessage)
            throws ItemNotFound, ValidationError, PermissionDenied;

    /**
     * Get an item's annotations.
     *
     * @param id the target item
     * @return an iterable of item annotations
     */
    Iterable<Annotation> getAnnotations(String id);

    /**
     * Create an annotation for a dependent node of an item. The entity and the
     * dependent item can be the same.
     *
     * @param id           the identifier of the Accessible this annotation is attached to, as a target
     * @param did          the identifier of the dependent item
     * @param bundle       the annotation itself
     * @param accessibleTo users or groups who can access this annotation
     * @param logMessage   a log message
     * @return the created annotation
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if the input data is incomplete or does not meet requirements
     * @throws AccessDenied     if the user cannot access the target item
     */
    Annotation createAnnotation(String id, String did, Bundle bundle,
                                Collection<Accessor> accessibleTo, Optional<String> logMessage)
            throws PermissionDenied, AccessDenied, ValidationError, ItemNotFound;

    /**
     * Up vote an item, removing a down vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws NotPromotableError if the item is not promotable
     */
    Promotable promote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove an up vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    Promotable removePromotion(String id) throws ItemNotFound, PermissionDenied;

    /**
     * Down vote an item, removing an up vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound       if the item does not exist
     * @throws PermissionDenied   if the user cannot perform the action
     * @throws NotPromotableError if the item is not promotable
     */
    Promotable demote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove a down vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     * @throws ItemNotFound     if the item does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    Promotable removeDemotion(String id) throws ItemNotFound, PermissionDenied;

    /**
     * Determine if logging is enabled.
     *
     * @return whether or not logging is enabled
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
         * @return the new inherited global permission set
         * @throws PermissionDenied if the grant action is not available to the user
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
         * @throws PermissionDenied if the grant action is not available to the user
         */
        InheritedItemPermissionSet setItemPermissions(Accessible item, Accessor accessor,
                                                      Set<PermissionType> permissionList) throws PermissionDenied;

        /**
         * Revoke a permission grant.
         *
         * @param grant the permission grant
         * @throws PermissionDenied if the grant action is not available to the user
         */
        void revokePermissionGrant(PermissionGrant grant) throws PermissionDenied;

        /**
         * Add a user to a group. This confers any permissions the group
         * has on the user, so requires grant permissions for the user as
         * we as modify permissions for the group.
         *
         * @param group       the group
         * @param userOrGroup the user to add to the group
         * @throws PermissionDenied if the grant action is not available to the user
         */
        void addAccessorToGroup(Group group, Accessor userOrGroup) throws PermissionDenied;

        /**
         * Remove a user from a group. Just as with adding uers, this requires
         * grant permissions for the user and modify permissions for the group.
         *
         * @param group       the group
         * @param userOrGroup the user to add to the group
         * @throws PermissionDenied if the grant action is not available to the user
         */
        void removeAccessorFromGroup(Group group, Accessor userOrGroup) throws PermissionDenied;
    }

    class NotPromotableError extends Exception {
        public NotPromotableError(String itemId) {
            super(String.format("Item '%s' is not marked as promotable.", itemId));
        }
    }
}
