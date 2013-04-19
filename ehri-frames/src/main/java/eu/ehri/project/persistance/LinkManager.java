package eu.ehri.project.persistance;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.AnnotatableEntity;

import java.util.List;
import java.util.Set;

/**
 * Class used to encapsulate functionality related to
 * linking items via various means.
 *
 * User: michaelb
 */
public class LinkManager {
    private FramedGraph<?> graph;
    private GraphManager manager;

    public LinkManager(FramedGraph<?> graph) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    public Optional<Vertex> getLinkedItem(AnnotatableEntity item, UndeterminedRelationship relationship) {
        for (Annotation annotation : relationship.getLinkedAnnotations()) {
            for (AnnotatableEntity entity : annotation.getTargets()) {
                if (!entity.asVertex().equals(item.asVertex())) {
                    return Optional.fromNullable(entity.asVertex());
                }
            }
        }

        // Damn, try and go the other way, from the
        // item - annotation-with-rel-body - other item
        // Remove this when linking is better figured out.
        for (Annotation annotation : item.getAnnotations()) {
            AnnotatableEntity source = annotation.getSource();
            if (source != null && manager.getEntityClass(source).equals(EntityClass.UNDETERMINED_RELATIONSHIP)) {
                for (AnnotatableEntity otherItem : annotation.getTargets()) {
                    if (!otherItem.asVertex().equals(item.asVertex())) {
                        return Optional.fromNullable(otherItem.asVertex());
                    }
                }
            }
        }

        return Optional.absent();
    }
}
