package eu.ehri.project.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

public class AclViews<E extends AccessibleEntity> extends AbstractViews<E> {

    public AclViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        super(graph, cls);
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
    public PermissionGrant setPermission(Long item, Long user,
            String permissionId) throws PermissionDenied, ValidationError,
            SerializationError {
        E entity = graph.getVertex(item, cls);
        checkEntityPermission(entity, user, PermissionTypes.GRANT);
        return acl.grantPermissions(getAccessor(user), entity,
                getPermission(permissionId), scope);
    }

    public PermissionGrant setGlobalPermissionMatrix(Long item, Long user,
            String permissionId) throws PermissionDenied, ValidationError,
            SerializationError {
        E entity = graph.getVertex(item, cls);
        checkEntityPermission(entity, user, PermissionTypes.GRANT);
        return acl.grantPermissions(getAccessor(user), entity,
                getPermission(permissionId), scope);
    }

    public void setAccessors(Long item, Set<Long> accessors, Long user)
            throws PermissionDenied {
        E entity = graph.getVertex(item, cls);
        checkEntityPermission(entity, user, PermissionTypes.UPDATE);
        // FIXME: Must be a more efficient way to do this, whilst
        // ensuring that superfluous double relationships don't get created?
        Set<Long> existing = new HashSet<Long>();
        Set<Long> remove = new HashSet<Long>();
        for (Accessor accessor : entity.getAccessors()) {
            Long id = (Long) accessor.asVertex().getId();
            existing.add(id);
            if (!accessors.contains(id)) {
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
        for (Long accessorId : accessors) {
            if (!existing.contains(accessorId)) {
                entity.addAccessor(graph.getVertex(accessorId, Accessor.class));
            }
        }
    }
}
