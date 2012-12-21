package eu.ehri.project.views.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.Vertex;
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
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.views.Acl;
import eu.ehri.project.views.ViewHelper;

public final class AclViews<E extends AccessibleEntity> implements Acl<E> {

    private final FramedGraph<Neo4jGraph> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final PermissionScope scope;
    private final GraphManager manager;

    /**
     * Scoped constructor.
     * 
     * @param graph
     * @param cls
     * @param scope
     */
    public AclViews(FramedGraph<Neo4jGraph> graph, Class<E> cls,
            PermissionScope scope) {
        this.graph = graph;
        this.scope = scope;
        acl = new AclManager(graph);
        helper = new ViewHelper(graph, cls, scope);
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor with system scope.
     * 
     * @param graph
     * @param cls
     */
    public AclViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Set permissions on a PermissionTarget.
     * 
     * @param entity
     * @param user
     * @param permissionType
     * @return
     * @throws PermissionDenied
     */
    public PermissionGrant setPermission(E entity, Accessor user,
            PermissionType permissionType) throws PermissionDenied {
        helper.checkEntityPermission(entity, user, PermissionType.GRANT);
        return acl.grantPermissions(user, entity, permissionType, scope);
    }

    /**
     * Set the global permission matrix for a user.
     * 
     * @param accessor
     * @param grantee
     * @param matrix
     * 
     * @throws PermissionDenied
     */
    public void setGlobalPermissionMatrix(Accessor accessor, Accessor grantee,
            Map<ContentTypes, List<PermissionType>> permissionMap)
            throws PermissionDenied {
        try {
            checkGrantPermission(grantee, permissionMap);
            acl.setGlobalPermissionMatrix(accessor, permissionMap);
            // Log the action...
            new ActionManager(graph).createAction(
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

    public void setAccessors(E entity, Set<Accessor> accessors, Accessor user)
            throws PermissionDenied {
        try {
            helper.checkEntityPermission(entity, user, PermissionType.UPDATE);
            // FIXME: Must be a more efficient way to do this, whilst
            // ensuring that superfluous double relationships don't get created?
            Set<Vertex> accessorVertices = Sets.newHashSet();
            for (Accessor acc : accessors)
                accessorVertices.add(acc.asVertex());

            Set<Vertex> existing = Sets.newHashSet();
            Set<Vertex> remove = Sets.newHashSet();
            for (Accessor accessor : entity.getAccessors()) {
                Vertex v = accessor.asVertex();
                existing.add(v);
                if (!accessorVertices.contains(v)) {
                    remove.add(v);
                }
            }
            for (Vertex v : remove) {
                entity.removeAccessor(graph.frame(v, Accessor.class));
            }
            for (Accessor accessor : accessors) {
                if (!existing.contains(accessor.asVertex())) {
                    entity.addAccessor(accessor);
                }
            }
            // Log the action...
            new ActionManager(graph).createAction(
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
                        throw new PermissionDenied(accessor, target, grantPerm,
                                SystemScope.getInstance());
                    }
                }
            } catch (ItemNotFound e) {
                throw new RuntimeException(
                        "Unable to get node for permission type '"
                                + PermissionType.GRANT + "'", e);
            }
        }
    }

    public void setScopedPermissions(Accessor accessor, Accessor grantee,
            ContentTypes contentType, List<PermissionType> enumifyPermissionList)
            throws PermissionDenied {
        try {
            helper.checkEntityPermission(scope, grantee, PermissionType.GRANT);
            for (PermissionType t : enumifyPermissionList) {
                acl.grantPermissions(accessor, acl.getContentType(contentType),
                        t, scope);
            }
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (PermissionDenied e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }
}
