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

import com.google.common.base.Preconditions;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Views class for permission operations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class AclViews implements Scoped<AclViews> {

    private final FramedGraph<?> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final ActionManager actionManager;

    /**
     * Scoped constructor.
     * 
     * @param graph The graph
     * @param scope The ACL scope
     */
    public AclViews(FramedGraph<?> graph, PermissionScope scope) {
        Preconditions.checkNotNull(scope);
        this.graph = graph;
        this.scope = scope;
        helper = new ViewHelper(graph, scope);
        acl = helper.getAclManager();
        manager = GraphManagerFactory.getInstance(graph);
        actionManager = new ActionManager(graph, scope);
    }

    /**
     * Constructor with system scope.
     * 
     * @param graph The graph
     */
    public AclViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Set the global permission matrix for a user.
     *
     * @param accessor The user
     * @param permissionSet The new permissions
     * @param grantee The user/group granting the new permissions
     * @throws PermissionDenied
     */
    public InheritedGlobalPermissionSet setGlobalPermissionMatrix(Accessor accessor,
                                       GlobalPermissionSet permissionSet,
            Accessor grantee) throws PermissionDenied {
        checkGrantPermission(grantee, permissionSet);
        acl.setPermissionMatrix(accessor, permissionSet);
        boolean scoped = !scope.equals(SystemScope.INSTANCE);
        // Log the action...
        ActionManager.EventContext context = actionManager.newEventContext(
                manager.cast(accessor, AccessibleEntity.class),
                manager.cast(grantee, Actioner.class),
                EventTypes.setGlobalPermissions);
        if (scoped) {
            context.addSubjects(manager.cast(scope, AccessibleEntity.class));
        }
        context.commit();
        return acl.getInheritedGlobalPermissions(accessor);
    }

    /**
     * Set accessors for a given resource. If the given accessor set is empty
     * the resource will be globally visible.
     * 
     * @param entity The item
     * @param accessors The list of users/groups who can access the item
     * @param user The user making the change
     */
    public void setAccessors(AccessibleEntity entity, Set<Accessor> accessors,
            Accessor user) throws PermissionDenied {
        helper.checkEntityPermission(entity, user, PermissionType.UPDATE);
        acl.setAccessors(entity, accessors);
        // Log the action...
        actionManager.newEventContext(
                entity, manager.cast(user, Actioner.class), EventTypes.setVisibility)
                .commit();
    }

    /**
     * Check the accessor has GRANT permissions to update another user's
     * permissions.
     * 
     * @param accessor The user
     * @param permissionSet The user's permissions.
     * @throws PermissionDenied
     */
    private void checkGrantPermission(Accessor accessor,
            GlobalPermissionSet permissionSet)
            throws PermissionDenied {
        Map<ContentTypes, Collection<PermissionType>> permissionMap
                = permissionSet.asMap();
        // Check we have grant permissions for the requested content types
        if (!acl.belongsToAdmin(accessor)) {
            try {
                Permission grantPerm = manager.getFrame(
                        PermissionType.GRANT.getName(), Permission.class);
                for (ContentTypes ctype : permissionMap.keySet()) {
                    if (!acl.hasPermission(ctype, PermissionType.GRANT, accessor)) {
                        throw new PermissionDenied(accessor.getId(),
                                ctype.toString(), grantPerm.getId(),
                                scope.getId());
                    }
                }
            } catch (ItemNotFound e) {
                throw new RuntimeException(
                        "Unable to get node for permission type '"
                                + PermissionType.GRANT + "'", e);
            }
        }
    }

    /**
     * Set permissions for the given user on the given item.
     * 
     * @param item The item
     * @param accessor The accessor
     * @param permissionList The set of permissions to grant
     * @param grantee The user doing the granting
     * 
     * @throws PermissionDenied
     */
    public void setItemPermissions(AccessibleEntity item, Accessor accessor,
            Set<PermissionType> permissionList, Accessor grantee)
            throws PermissionDenied {
        helper.checkEntityPermission(item, grantee, PermissionType.GRANT);
        acl.setItemPermissions(item, accessor, permissionList);
        // Log the action...
        actionManager.newEventContext(item,
                manager.cast(grantee, Actioner.class), EventTypes.setItemPermissions)
                .addSubjects(manager.cast(accessor, AccessibleEntity.class))
                .commit();
    }

    public void revokePermissionGrant(PermissionGrant grant, Accessor user)
            throws PermissionDenied {
        // TODO: This should be rather more complicated and account for the
        // fact that individual grants can, in theory, have more than one
        // target content type.
        for (PermissionGrantTarget tg : grant.getTargets()) {
            switch (manager.getEntityClass(tg)) {
                case CONTENT_TYPE:
                    helper.checkContentPermission(user, ContentTypes.withName(tg.getId()), PermissionType.GRANT);
                    break;
                default:
                    helper.checkEntityPermission(
                        manager.cast(tg, AccessibleEntity.class), user, PermissionType.GRANT);
            }
        }
        acl.revokePermissionGrant(grant);
    }

    /**
     * Add a user to a group. This confers any permissions the group
     * has on the user, so requires grant permissions for the user as
     * we as modify permissions for the group.
     *
     * @param group The group
     * @param user The user to add to the group
     * @param grantee The user performing the action
     * @throws PermissionDenied
     */
    public void addAccessorToGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        ensureCanModifyGroupMembership(group, user, grantee);
        group.addMember(user);
        // Log the action...
        actionManager.newEventContext(group,
                manager.cast(grantee, Actioner.class), EventTypes.addGroup)
                .addSubjects(manager.cast(user, AccessibleEntity.class))
                .commit();
    }

    /**
     * Remove a user from a group. Just as with adding uers, this requires
     * grant permissions for the user and modify permissions for the group.
     *
     * @param group The group
     * @param user The user to add to the group
     * @param grantee The user performing the action
     * @throws PermissionDenied
     */
    public void removeAccessorFromGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        ensureCanModifyGroupMembership(group, user, grantee);
        group.removeMember(user);
        // Log the action...
        actionManager.newEventContext(group,
                manager.cast(grantee, Actioner.class), EventTypes.removeGroup)
                .addSubjects(manager.cast(user, AccessibleEntity.class))
                .commit();
    }

    private void ensureCanModifyGroupMembership(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        // If a user is not admin they can only add someone else to a group if
        // a) they belong to that group themselves, and
        // b) they have the modify permission on that group, and
        // c) they have grant permissions for the user
        if (!acl.belongsToAdmin(grantee)) {
            boolean found = false;
            for (Accessor acc : grantee.getAllParents()) {
                if (group.equals(acc)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new PermissionDenied(grantee.getId(), group.getId(),
                        "Non-admin users cannot add other users to groups that they" +
                                " do not themselves belong to.");
            }
            helper.checkEntityPermission(manager.cast(user, AccessibleEntity.class),
                    grantee, PermissionType.GRANT);
            helper.checkEntityPermission(group, grantee, PermissionType.UPDATE);
        }
    }

    @Override
    public AclViews withScope(PermissionScope scope) {
        return new AclViews(graph, scope);
    }
}
