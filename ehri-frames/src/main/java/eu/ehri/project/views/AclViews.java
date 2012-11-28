package eu.ehri.project.views;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

public final class AclViews<E extends AccessibleEntity> {

    private final FramedGraph<Neo4jGraph> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final PermissionScope scope;

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
    }

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public AclViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this(graph, cls, new SystemScope());
    }

    /**
     * Set permissions on a PermissionTarget.
     * 
     * @param target
     *            target id
     * @param accessor
     *            accessor id
     * @param permission
     *            permission
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public PermissionGrant setPermission(E entity, Accessor user,
            String permissionId) throws PermissionDenied, ValidationError,
            SerializationError {
        helper.checkEntityPermission(entity, user,
                helper.getPermission(PermissionTypes.GRANT));
        return acl.grantPermissions(user, entity,
                helper.getPermission(permissionId), scope);
    }

    public void setGlobalPermissionMatrix(Accessor accessor, Accessor grantee,
            Map<String, List<String>> permissionMap) throws PermissionDenied,
            ValidationError, SerializationError {

        // Check we have grant permissions for the requested content types
        if (!acl.belongsToAdmin(grantee)) {
            try {
                Permission grantPerm = helper.getEntity(EntityTypes.PERMISSION,
                        PermissionTypes.GRANT, Permission.class);
                for (String ctype : permissionMap.keySet()) {
                    ContentType target = helper.getContentType(ctype);
                    Iterable<PermissionGrant> grants = acl.getPermissionGrants(
                            grantee, target, grantPerm);
                    if (!grants.iterator().hasNext()) {
                        throw new PermissionDenied(grantee, target, grantPerm,
                                new SystemScope());
                    }
                }
            } catch (ItemNotFound e) {
                throw new RuntimeException(
                        "Unable to get node for permission type '"
                                + PermissionTypes.GRANT + "'", e);
            }
        }

        acl.setGlobalPermissionMatrix(accessor, permissionMap);
    }

    public void setAccessors(E entity, Set<Accessor> accessors, Accessor user)
            throws PermissionDenied {
        helper.checkEntityPermission(entity, user,
                helper.getPermission(PermissionTypes.UPDATE));
        // FIXME: Must be a more efficient way to do this, whilst
        // ensuring that superfluous double relationships don't get created?
        Set<Long> accessorIds = new HashSet<Long>();
        for (Accessor acc : accessors)
            accessorIds.add((Long) acc.asVertex().getId());

        Set<Long> existing = new HashSet<Long>();
        Set<Long> remove = new HashSet<Long>();
        for (Accessor accessor : entity.getAccessors()) {
            Long id = (Long) accessor.asVertex().getId();
            existing.add(id);
            if (!accessorIds.contains(id)) {
                remove.add(id);
            }
        }
        for (Long removeId : remove) {
            // FIXME: The Blueprints remove behaviour is strange. It seems
            // to open a new transaction for every delete operation, which
            // then requires closing explicitly. Check this out.
            entity.removeAccessor(graph.getVertex(removeId, Accessor.class));
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        }
        for (Accessor accessor : accessors) {
            if (!existing.contains((Long) accessor.asVertex().getId())) {
                entity.addAccessor(accessor);
            }
        }
    }
}
