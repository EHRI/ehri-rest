package eu.ehri.project.views;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships.Access;
import eu.ehri.project.views.ObjectToRepresentationConverter;
import eu.ehri.project.acl.Acl;

public class CrudAcl {
    public String detail(FramedGraph<Neo4jGraph> graph, Long item, Long user)
            throws PermissionDenied {
        AccessibleEntity entity = graph.getVertex(item, AccessibleEntity.class);
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Access access = Acl.getAccessControl(entity, accessor);
        if (!access.getRead())
            throw new PermissionDenied(accessor, entity);
        return ObjectToRepresentationConverter.convert(entity);
    }
}
