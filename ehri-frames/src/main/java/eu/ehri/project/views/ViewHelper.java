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

package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Messy stopgap class to hold a bunch of sort-of view/sort-of acl functions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class ViewHelper {

    private final FramedGraph<?> graph;
    private final PermissionScope scope;
    private final AclManager acl;
    private final GraphManager manager;

    public ViewHelper(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    public ViewHelper(FramedGraph<?> graph, PermissionScope scope) {
        Preconditions.checkNotNull(scope);
        this.graph = graph;
        this.acl = new AclManager(graph, scope);
        this.scope = scope;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Check permissions for a given type.
     *
     * @param accessor The user/group
     * @param contentType The content type
     * @param permissionType The permission type
     * @throws PermissionDenied
     */
    public void checkContentPermission(Accessor accessor, ContentTypes contentType,
            PermissionType permissionType) throws PermissionDenied {
        if (!acl.hasPermission(contentType, permissionType, accessor)) {
            throw new PermissionDenied(accessor.getId(), contentType.toString(), permissionType.toString(), scope.getId());
        }
    }

    /**
     * Check permissions for a given entity.
     *
     * @param entity The item
     * @param accessor The user/group
     * @param permissionType The permission type
     * @throws PermissionDenied
     */
    public void checkEntityPermission(AccessibleEntity entity,
            Accessor accessor, PermissionType permissionType) throws PermissionDenied {
        if (!acl.hasPermission(entity, permissionType, accessor)) {
            throw new PermissionDenied(accessor.getId(), entity.getId(),
                        permissionType.toString(), scope.getId());
        }
    }

    /**
     * Ensure an item is readable by the given accessor
     *
     * @param entity The item
     * @param accessor The accessor/group
     * @throws AccessDenied
     */
    public void checkReadAccess(AccessibleEntity entity, Accessor accessor)
            throws AccessDenied {
        if (!acl.canAccess(entity, accessor)) {
            // Using 'fake' permission 'read'
            throw new AccessDenied(accessor.getId(), entity.getId());
        }
    }

    /**
     * Get the content type node with the given id.
     *
     * @param entityClass The entity class
     * @return A vertex framed as a content entityClass
     */
    public ContentType getContentTypeNode(EntityClass entityClass) {
        try {
            return manager.getFrame(entityClass.getName(), ContentType.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(
                    String.format("No content entityClass node found for entityClass: '%s'",
                            entityClass.getName()), e);
        }
    }

    /**
     * Get the content type enum value with the given id.
     *
     * @param cls The frame class
     * @return A vertex framed as a content type
     */
    public ContentTypes getContentTypeEnum(Class<?> cls) {
        return ContentTypes.withName(ClassUtils.getEntityType(cls).getName());
    }

    /**
     * @return the acl
     */
    public AclManager getAclManager() {
        return acl;
    }

    /**
     * Set the scope under which ACL and permission operations will take place.
     * This is, for example, an Repository instance, where the objects being
     * manipulated are DocumentaryUnits. The given scope is used to compare
     * against the scope relation on PermissionGrants.
     *
     * @param scope The new scope
     * @return A new view helper
     */
    public ViewHelper setScope(PermissionScope scope) {
        return new ViewHelper(graph, Optional
                .fromNullable(scope).or(SystemScope.INSTANCE));
    }
}
