package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.BundlePersister;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.relationships.Access;
import eu.ehri.project.acl.Acl;

public class Views<E extends AccessibleEntity> {

    private final FramedGraph<Neo4jGraph> graph;
    private final Class<E> cls;

    /**
     * @param graph
     * @param cls
     */
    public Views(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
    }

    public String detail(Long item, Long user) throws PermissionDenied {
        E entity = graph.getVertex(item, cls);
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = Acl.getAccessControl(entity, accessor);
        if (!access.getRead())
            throw new PermissionDenied(accessor, entity);
        return ObjectToRepresentationConverter.convert(entity);
    }

    public String update(Map<String,Object> data, Long user)
            throws PermissionDenied, ValidationError {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        EntityBundle<E> bundle = RepresentationToObjectConverter.dataToBundle(data);
        if (bundle.getId() == null)
            throw new PermissionDenied(accessor,
                    "No identifier given for updating object.");
        E entity = graph.getVertex(bundle.getId(), cls);
        Access access = Acl.getAccessControl(entity, accessor);
        if (!(access.getRead() && access.getWrite()))
            throw new PermissionDenied(accessor, entity);
        BundlePersister<E> persister = new BundlePersister<E>(graph);
        return ObjectToRepresentationConverter.convert(persister
                .persist(bundle));
    }

    public String create(Map<String,Object> data, Long user)
            throws PermissionDenied, ValidationError {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        if (false /*
                   * Somehow check a user's global and/or repository
                   * permissions.
                   */)
            throw new PermissionDenied(accessor,
                    "No global write permissions present.");
        EntityBundle<E> bundle = RepresentationToObjectConverter.dataToBundle(data);
        if (bundle.getId() != null)
            throw new PermissionDenied(accessor,
                    "Existing identifier given when creating object.");
        BundlePersister<E> persister = new BundlePersister<E>(graph);
        return ObjectToRepresentationConverter.convert(persister.persist(bundle));
    }

    public Integer delete(Long item, Long user) throws PermissionDenied, ValidationError {
        E entity = graph.getVertex(item, cls);
        Accessor accessor = graph.getVertex(user, Accessor.class);
        if (false /*
                   * Somehow check a user's global and/or repository
                   * permissions.
                   */)
            throw new PermissionDenied(accessor,
                    "No global delete permissions present.");
        Access access = Acl.getAccessControl(entity, accessor);
        if (!(access.getRead() && access.getWrite()))
            throw new PermissionDenied(accessor, entity);

        BundlePersister<E> persister = new BundlePersister<E>(graph);
        return persister.delete(ObjectToRepresentationConverter.vertexFrameToBundle(entity));
    }
}
