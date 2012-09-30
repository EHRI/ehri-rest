package eu.ehri.project.views;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.relationships.Access;

abstract class AbstractViews<E extends AccessibleEntity> {

    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;

    /**
     * @param graph
     * @param cls
     */
    public AbstractViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }


    /**
     * Ensure an item is readable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkReadAccess(AccessibleEntity entity, long user)
            throws PermissionDenied {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = acl.getAccessControl(entity, accessor);
        if (!access.getRead())
            throw new PermissionDenied(accessor, entity);
    }

    /**
     * Ensure an item is writable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, long user)
            throws PermissionDenied {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = acl.getAccessControl(entity, accessor);
        if (!(access.getRead() && access.getWrite()))
            throw new PermissionDenied(accessor, entity);
    }

    protected void checkGlobalWriteAccess(long user) throws PermissionDenied {
        // TODO: Stub
    }


}
