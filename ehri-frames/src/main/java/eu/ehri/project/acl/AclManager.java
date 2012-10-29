package eu.ehri.project.acl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Helper class for checking and asserting access and write permissions.
 * 
 * @author mike
 * 
 */
public class AclManager {

    private FramedGraph<Neo4jGraph> graph;

    public AclManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        new GraphHelpers(graph.getBaseGraph().getRawGraph());
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     */
    public Boolean isAdmin(Accessor accessor) {
        assert accessor != null : "Accessor is null";
        if (accessor.getName().equals(Group.ADMIN_GROUP_NAME))
            return true;
        for (Accessor acc : accessor.getAllParents()) {
            if (acc.getName().equals(Group.ADMIN_GROUP_NAME))
                return true;
        }
        return false;
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     */
    public Boolean isAnonymous(Accessor accessor) {
        return accessor instanceof AnonymousAccessor;
    }

    /*
     * We have to ascend the current accessors group hierarchy looking for a
     * groups that are contained in the current entity's ACL list.
     */
    public List<Accessor> searchAccess(List<Accessor> accessing,
            List<Accessor> allowedAccessors) {
        if (accessing.isEmpty()) {
            return new ArrayList<Accessor>();
        } else {
            List<Accessor> intersection = new ArrayList<Accessor>();
            for (Accessor acc : allowedAccessors) {
                if (accessing.contains(acc)) {
                    intersection.add(acc);
                }
            }

            List<Accessor> parentPerms = new ArrayList<Accessor>();
            parentPerms.addAll(intersection);
            for (Accessor acc : accessing) {
                List<Accessor> parents = new ArrayList<Accessor>();
                for (Accessor parent : acc.getAllParents())
                    parents.add(parent);
                parentPerms.addAll(searchAccess(parents, allowedAccessors));
            }

            return parentPerms;
        }
    }

    /**
     * Find the access permissions for a given accessor and entity.
     * 
     * @param entity
     * @param accessor
     * 
     * @return
     * @return
     */
    public boolean getAccessControl(AccessibleEntity entity, Accessor accessor) {
        // Admin can read/write everything and object can always read/write
        // itself
        assert entity != null : "Entity is null";
        assert accessor != null : "Accessor is null";
        // FIXME: Tidy up the logic here.
        if (isAdmin(accessor)
                || (!isAnonymous(accessor) && accessor.asVertex().equals(
                        entity.asVertex()))) {
            return true;
        }

        // Otherwise, check if there are specified permissions.
        List<Accessor> accessors = new ArrayList<Accessor>();
        for (Accessor acc : entity.getAccessors()) {
            accessors.add(acc);
        }

        if (accessors.isEmpty()) {
            return true;
        } else if (isAnonymous(accessor)) {
            return false;
        } else {
            List<Accessor> initial = new ArrayList<Accessor>();
            initial.add(accessor);
            return !searchAccess(initial, accessors).isEmpty();
        }
    }

    /**
     * Get the permissions for a given accessor on a given entity.
     * 
     * @param accessor
     * @param entity
     * @param permission
     */
    public Iterable<PermissionGrant> getPermissions(Accessor accessor,
            PermissionGrantTarget target, Permission permission) {
        List<PermissionGrant> grants = new LinkedList<PermissionGrant>();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            for (PermissionGrantTarget t : grant.getTargets()) {
                if (((grant.getPermission().getMask() & permission.getMask()) == permission
                        .getMask()) && target.asVertex().equals(t.asVertex())) {
                    grants.add(grant);
                }
            }
        }

        for (Accessor parent : accessor.getParents()) {
            for (PermissionGrant grant : getPermissions(parent, target,
                    permission)) {
                grants.add(grant);
            }
        }
        return ClassUtils.makeUnique(grants);
    }

    /**
     * Grant a user permissions to a content type.
     * 
     * @param accessor
     * @param contentType
     * @param permission
     * @return
     */
    public PermissionGrant grantPermissions(Accessor accessor,
            PermissionGrantTarget target, Permission permission) {
        PermissionGrant grant = graph.addVertex(null, PermissionGrant.class);
        accessor.addPermissionGrant(grant);
        grant.setPermission(permission);
        grant.addTarget(target);
        return grant;
    }

    /**
     * Grant a user permissions to a content type, with the given scope.
     * 
     * @param accessor
     * @param contentType
     * @param permission
     * @param scope
     * @return
     */
    public PermissionGrant grantPermissions(Accessor accessor,
            PermissionGrantTarget target, Permission permission,
            PermissionScope scope) {
        PermissionGrant grant = grantPermissions(accessor, target, permission);
        if (!scope.getIdentifier().equals(SystemScope.SYSTEM))
            grant.addScope(scope);
        return grant;
    }

    /**
     * Revoke a particular permission grant.
     * 
     * @param accessor
     * @param target
     * @param permission
     */
    public void revokePermissions(Accessor accessor,
            PermissionGrantTarget target, Permission permission) {
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (grant.getPermission().equals(permission)) {
                for (PermissionGrantTarget tg : grant.getTargets()) {
                    if (tg.asVertex().equals(target.asVertex())) {
                        grant.removeTarget(tg);
                    }
                }
                // Clean up if there are no more targets...
                if (!grant.getTargets().iterator().hasNext()) {
                    graph.removeVertex(grant.asVertex());
                }                
            }
        }
    }

    /**
     * Set access control on an entity.
     * 
     * @param entity
     * @param accessor
     * @param canRead
     * @param canWrite
     * @throws PermissionDenied
     */
    public void setAccessControl(AccessibleEntity entity, Accessor accessor)
            throws PermissionDenied {
        entity.addAccessor(accessor);
    }

    /**
     * Revoke an accessors access to an entity.
     * 
     * @param entity
     * @param accessor
     */
    public void removeAccessControl(AccessibleEntity entity, Accessor accessor) {
        entity.removeAccessor(accessor);
    }

    /**
     * Set access control on an entity to several accessors.
     * 
     * @param entity
     * @param accessors
     * @param canRead
     * @param canWrite
     * @throws PermissionDenied
     */
    public void setAccessControl(AccessibleEntity entity, Accessor[] accessors)
            throws PermissionDenied {
        for (Accessor accessor : accessors)
            entity.addAccessor(accessor);
    }

    /**
     * Build a gremlin filter function that passes through items readable by a
     * given accessor.
     * 
     * @param accessor
     * @return
     */
    public PipeFunction<Vertex, Boolean> getAclFilterFunction(Accessor accessor) {
        assert accessor != null;
        if (isAdmin(accessor))
            return noopFilterFunction();

        final HashSet<Object> all = getAllAccessors(accessor);
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex v) {
                Iterable<Vertex> verts = v.getVertices(Direction.OUT,
                        AccessibleEntity.ACCESS);
                // If there's no Access conditions, it's
                // read-only...
                if (!verts.iterator().hasNext())
                    return true;
                for (Vertex other : verts) {
                    if (all.contains(other.getId()))
                        return true;
                }
                return false;
            }
        };
    }

    /**
     * Pipe filter function that passes through all items. TODO: Check if we
     * actually need this???
     * 
     * @return
     */
    private PipeFunction<Vertex, Boolean> noopFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex v) {
                return true;
            }
        };
    }

    /**
     * For a given user, fetch a lookup of all the inherited accessors it
     * belongs to.
     * 
     * @param user
     * @return
     */
    private HashSet<Object> getAllAccessors(Accessor accessor) {

        final HashSet<Object> all = new HashSet<Object>();
        if (!isAnonymous(accessor)) {
            Iterable<Accessor> parents = accessor.getAllParents();
            for (Accessor a : parents)
                all.add(a.asVertex().getId());
            all.add(accessor.asVertex().getId());
        }
        return all;
    }
}
