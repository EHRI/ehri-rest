package eu.ehri.project.acl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.relationships.Access;

public class AclManager {

    private FramedGraph<Neo4jGraph> graph;

    public AclManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        new GraphHelpers(graph.getBaseGraph().getRawGraph());
    }

    static class AccessComparator implements Comparator<Access> {
        // FIXME: Find a better, more elegant way of doing this comparison
        // between two access objects. We can't just implement a custom equals
        // method because we might sometimes be comparing an Edge interface, and
        // other times a synthetic factory-made object. Suggestions on a
        // postcard.
        public int compare(Access a, Access b) {
            if (a.getWrite() == b.getWrite() && a.getRead() == b.getRead())
                return 0;
            else if (a.getWrite() && b.getWrite() && a.getRead()
                    && !b.getRead())
                return 1;
            else if (a.getWrite() && !b.getWrite() && a.getRead()
                    && !b.getRead())
                return 1;
            else
                return -1;
        }
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
     * groups that are contained in the current entity's ACL list. Return the
     * Access relationship objects and see which one is most liberal.
     */
    public List<Access> searchPermissions(List<Accessor> accessing,
            List<Access> allowedCtrls, List<Accessor> allowedAccessors) {
        assert allowedCtrls.size() == allowedAccessors.size();
        if (accessing.isEmpty()) {
            return new ArrayList<Access>();
        } else {
            List<Access> intersection = new ArrayList<Access>();
            for (int i = 0; i < allowedCtrls.size(); i++) {
                if (accessing.contains(allowedAccessors.get(i))) {
                    intersection.add(allowedCtrls.get(i));
                }
            }

            List<Access> parentPerms = new ArrayList<Access>();
            parentPerms.addAll(intersection);
            for (Accessor acc : accessing) {
                List<Accessor> parents = new ArrayList<Accessor>();
                for (Accessor parent : acc.getAllParents())
                    parents.add(parent);
                parentPerms.addAll(searchPermissions(parents, allowedCtrls,
                        allowedAccessors));
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
    public Access getAccessControl(AccessibleEntity entity, Accessor accessor) {
        // Admin can read/write everything and object can always read/write
        // itself

        assert entity != null : "Entity is null";
        assert accessor != null : "Accessor is null";

        // FIXME: Tidy up the logic here.
        if (isAdmin(accessor)
                || (!isAnonymous(accessor) && accessor.asVertex().equals(
                        entity.asVertex())))
            return new EntityAccessFactory().buildReadWrite(entity, accessor);

        // Otherwise, check if there are specified permissions.
        List<Access> accessCtrls = new ArrayList<Access>();
        List<Accessor> accessors = new ArrayList<Accessor>();
        for (Access access : entity.getAccess()) {
            accessCtrls.add(access);
            accessors.add(access.getAccessor());
        }

        if (accessCtrls.isEmpty()) {
            return new EntityAccessFactory().buildReadOnly(entity, accessor);
        } else if (isAnonymous(accessor)) {
            return new EntityAccessFactory().buildNoAccess(entity, accessor);
        } else {
            // If there are, search the Group hierarchy and find the most
            // powerful set of permissions contained therein...
            List<Accessor> initial = new ArrayList<Accessor>();
            initial.add(accessor);
            List<Access> ctrls = searchPermissions(initial, accessCtrls,
                    accessors);
            if (ctrls.isEmpty()) {
                return new EntityAccessFactory()
                        .buildNoAccess(entity, accessor);
            } else {
                return Collections.max(ctrls, new AccessComparator());
            }
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
            ContentType contentType, Permission permission) {
        List<PermissionGrant> grants = new LinkedList<PermissionGrant>();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (grant.getContentType().equals(contentType))
                grants.add(grant);
        }
            
        for (Accessor parent : accessor.getParents()) {
            for (PermissionGrant grant : getPermissions(parent, contentType,
                    permission)) {
                grants.add(grant);
            }
        }

        return ClassUtils.makeUnique(grants);
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
    public void setAccessControl(AccessibleEntity entity, Accessor accessor,
            boolean canRead, boolean canWrite) throws PermissionDenied {

        // Logic containment! Ensure people can't add write access
        // to the anonymous accessor
        if (canWrite && isAnonymous(accessor)) {
            throw new PermissionDenied(
                    "Cannot allow write access for anonymous user.");
        }

        // FIXME: There should be a better way of doing this...
        Edge edge = null;
        for (Edge e : entity.asVertex().getEdges(Direction.OUT,
                AccessibleEntity.ACCESS)) {
            if (e.getVertex(Direction.IN).equals(accessor.asVertex()))
                edge = e;
        }
        if (edge == null) {
            edge = graph.addEdge(null, entity.asVertex(), accessor.asVertex(),
                    AccessibleEntity.ACCESS);
        }
        Access access = graph.frame(edge, Direction.OUT, Access.class);
        access.setRead(canRead);
        access.setWrite(canWrite);
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
    public void setAccessControl(AccessibleEntity entity, Accessor[] accessors,
            boolean canRead, boolean canWrite) throws PermissionDenied {
        for (Accessor accessor : accessors)
            setAccessControl(entity, accessor, canRead, canWrite);
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
                Iterable<Edge> edges = v.getEdges(Direction.OUT,
                        AccessibleEntity.ACCESS);
                // If there's no Access conditions, it's
                // read-only...
                if (!edges.iterator().hasNext())
                    return true;
                for (Edge e : edges) {
                    // FIXME: Does not currently check the
                    // actual permission property. This assumes
                    // that if there's a permission, it means that
                    // the subject can be read.
                    Vertex other = e.getVertex(Direction.OUT);
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
