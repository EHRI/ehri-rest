/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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
import eu.ehri.project.models.AccessPointType;
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
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.persistence.VersionManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Optional;


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

    /**
     * Fetch an item, as a user. This only provides access control.
     *
     * @param id  the item id
     * @param cls the item's class
     * @return the given framed vertex
     */
    <E extends Accessible> E detail(String id, Class<E> cls) throws ItemNotFound;

    /**
     * Update an object bundle, also updating dependent items.
     *
     * @param bundle the item's data bundle
     * @param cls    the item's class
     * @return the updated framed vertex
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
     */
    <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError;

    /**
     * Delete an object bundle, following dependency cascades, within the scope
     * of item `scope`.
     *
     * @param id the item ID
     * @return the number of vertices deleted.
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
     */
    Link createLink(String targetId1, String targetId2, List<String> bodies, Bundle bundle,
            Collection<Accessor> accessibleTo, Optional<String> logMessage) throws ItemNotFound, ValidationError,
            PermissionDenied;

    /**
     * Create a link between two items, along with an access point on the given description.
     *
     * @param targetId1 the identifier of a Accessible target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle    the annotation itself
     * @return a new link
     */
    Link createAccessPointLink(String targetId1, String targetId2, String descriptionId, String bodyName,
            AccessPointType bodyType, Bundle bundle, Collection<Accessor> accessibleTo, Optional<String> logMessage)
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
     */

    Annotation createAnnotation(String id, String did, Bundle bundle,
            Collection<Accessor> accessibleTo, Optional<String> logMessage)
            throws PermissionDenied, AccessDenied, ValidationError, ItemNotFound;

    /**
     * Up vote an item, removing a down vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     */
    Promotable promote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove an up vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     */
    Promotable removePromotion(String id) throws ItemNotFound, PermissionDenied;

    /**
     * Down vote an item, removing an up vote if there is one.
     *
     * @param id the promotable item's ID
     * @return the promotable item
     */
    Promotable demote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError;

    /**
     * Remove a down vote.
     *
     * @param id the promotable item's ID
     * @return the promotable item
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
         */
        InheritedItemPermissionSet setItemPermissions(Accessible item, Accessor accessor,
                Set<PermissionType> permissionList)
                throws PermissionDenied;

        /**
         * Revoke a permission grant.
         *
         * @param grant the permission grant
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
         */
        void addAccessorToGroup(Group group, Accessor userOrGroup) throws PermissionDenied;

        /**
         * Remove a user from a group. Just as with adding uers, this requires
         * grant permissions for the user and modify permissions for the group.
         *
         * @param group       the group
         * @param userOrGroup the user to add to the group
         */
        void removeAccessorFromGroup(Group group, Accessor userOrGroup) throws PermissionDenied;
    }

    class NotPromotableError extends Exception {
        public NotPromotableError(String itemId) {
            super(String.format("Item '%s' is not marked as promotable.", itemId));
        }
    }
}
