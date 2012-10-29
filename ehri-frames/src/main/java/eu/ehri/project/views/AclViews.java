package eu.ehri.project.views;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;

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
}
