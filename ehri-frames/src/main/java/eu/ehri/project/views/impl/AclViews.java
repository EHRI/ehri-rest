package eu.ehri.project.views.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.views.Acl;
import eu.ehri.project.views.ViewHelper;

public final class AclViews implements Acl {

    private final FramedGraph<Neo4jGraph> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final GraphManager manager;
    private final PermissionScope scope;

    /**
     * Scoped constructor.
     * 
     * @param graph
     * @param scope
     */
    public AclViews(FramedGraph<Neo4jGraph> graph, PermissionScope scope) {
        this.graph = graph;
        helper = new ViewHelper(graph, scope);
        acl = helper.getAclManager();
        manager = GraphManagerFactory.getInstance(graph);
        this.scope = scope;
    }

    /**
     * Constructor with system scope.
     * 
     * @param graph
     */
    public AclViews(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Set the global permission matrix for a user.
     * 
     * @param accessor
     * @param permissionMap
     * @param grantee
     * @throws PermissionDenied
     */
    public void setGlobalPermissionMatrix(Accessor accessor,
            Map<ContentTypes, List<PermissionType>> permissionMap,
            Accessor grantee) throws PermissionDenied {
        try {
            checkGrantPermission(grantee, permissionMap);
            acl.setPermissionMatrix(accessor, permissionMap);
            // Log the action...
            new ActionManager(graph).logEvent(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Updated permissions");
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (PermissionDenied e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set accessors for a given resource. If the given accessor set is empty
     * the resource will be globally visible.
     * 
     * @param entity
     * @param accessors
     *            the list of accessors to whom this item is visible
     * @param user
     *            the user making the change
     */
    public void setAccessors(AccessibleEntity entity, Set<Accessor> accessors,
            Accessor user) throws PermissionDenied {
        try {
            helper.checkEntityPermission(entity, user, PermissionType.UPDATE);
            acl.setAccessors(entity, accessors);
            // Log the action...
            new ActionManager(graph).logEvent(
                    graph.frame(entity.asVertex(), AccessibleEntity.class),
                    graph.frame(user.asVertex(), Actioner.class),
                    "Set visibility");
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (PermissionDenied e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    /**
     * Check the accessor has GRANT permissions to update another user's
     * permissions.
     * 
     * @param accessor
     * @param permissionMap
     * @throws PermissionDenied
     */
    private void checkGrantPermission(Accessor accessor,
            Map<ContentTypes, List<PermissionType>> permissionMap)
            throws PermissionDenied {
        // Check we have grant permissions for the requested content types
        if (!acl.belongsToAdmin(accessor)) {
            try {
                Permission grantPerm = manager.getFrame(
                        PermissionType.GRANT.getName(), Permission.class);
                for (ContentTypes ctype : permissionMap.keySet()) {
                    ContentType target = manager.getFrame(ctype.getName(),
                            ContentType.class);
                    Iterable<PermissionGrant> grants = acl.getPermissionGrants(
                            accessor, target, grantPerm);
                    if (!grants.iterator().hasNext()) {
                        throw new PermissionDenied(manager.getId(accessor),
                                manager.getId(target), manager.getId(grantPerm),
                                "system");
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
     * @param item
     * @param accessor
     * @param permissionList
     * @param grantee
     * 
     * @throws PermissionDenied
     */
    public void setItemPermissions(AccessibleEntity item, Accessor accessor,
            Set<PermissionType> permissionList, Accessor grantee)
            throws PermissionDenied {
        try {
            helper.checkEntityPermission(item, grantee, PermissionType.GRANT);
            acl.setEntityPermissions(accessor, item, permissionList);
            // Log the action...
            new ActionManager(graph).logEvent(item,
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Modified item-level permissions").addSubjects(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class));

            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (PermissionDenied e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    public void revokePermissionGrant(PermissionGrant grant, Accessor user)
            throws PermissionDenied {
        // TODO: This should be rather more complicated and account for the
        // fact that individual grants can, in theory, have more than one
        // target content type.
        for (PermissionGrantTarget tg : grant.getTargets()) {
            helper.checkEntityPermission(
                    graph.frame(tg.asVertex(), AccessibleEntity.class), user,
                    PermissionType.GRANT);
        }
        acl.revokePermissionGrant(grant);
    }

    /**
     * Add a user to a group. This confers any permissions the group
     * has on the user, so requires grant permissions for the user as
     * we as modify permissions for the group.
     *
     * @param group
     * @param user
     * @param grantee
     * @throws PermissionDenied
     */
    public void addAccessorToGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        ensureCanModifyGroupMembership(group, user, grantee);
        try {
            group.addMember(graph.frame(user.asVertex(), Accessor.class));
            // Log the action...
            new ActionManager(graph).logEvent(group,
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Added user to group").addSubjects(
                    graph.frame(user.asVertex(), AccessibleEntity.class));
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }

    }

    /**
     * Remove a user from a group. Just as with adding uers, this requires
     * grant permissions for the user and modify permissions for the group.
     *
     * @param group
     * @param user
     * @param grantee
     * @throws PermissionDenied
     */
    public void removeAccessorFromGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        ensureCanModifyGroupMembership(group, user, grantee);
        try {
            group.removeMember(graph.frame(user.asVertex(), Accessor.class));
            // Log the action...
            new ActionManager(graph).logEvent(group,
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Removed user from group").addSubjects(
                    graph.frame(user.asVertex(), AccessibleEntity.class));
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    private void ensureCanModifyGroupMembership(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        // If a user is not admin they can only add someone else to a group if
        // a) they belong to that group themselves, and
        // b) they have the modify permission on that group, and
        // c) they have grant permissions for the user
        if (!acl.belongsToAdmin(grantee)) {
            // FIXME: With TP 2.3.0 update this comparing of vertices shouldn't be necessary
            boolean found = false;
            for (Accessor acc : grantee.getAllParents()) {
                if (group.asVertex().equals(acc.asVertex())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new PermissionDenied(grantee.getIdentifier(), group.getIdentifier(),
                        "Non-admin users cannot add other users to groups that they" +
                                " do not themselves belong to.");
            }
            helper.checkEntityPermission(graph.frame(user.asVertex(),
                    AccessibleEntity.class), grantee, PermissionType.GRANT);
            helper.checkEntityPermission(group, grantee, PermissionType.UPDATE);
        }
    }
}
