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
}
