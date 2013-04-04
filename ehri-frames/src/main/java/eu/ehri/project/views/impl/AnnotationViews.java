package eu.ehri.project.views.impl;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.persistance.TraversalCallback;
import eu.ehri.project.views.Annotations;
import eu.ehri.project.views.ViewHelper;

/**
 * View class for handling annotation-related operations.
 * 
 * @author mike
 */
public final class AnnotationViews implements Annotations {

    private final FramedGraph<Neo4jGraph> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final GraphManager manager;

    /**
     * Scoped constructor.
     * 
     * @param graph
     * @param scope
     */
    public AnnotationViews(FramedGraph<Neo4jGraph> graph, PermissionScope scope) {
        this.graph = graph;
        helper = new ViewHelper(graph, scope);
        acl = helper.getAclManager();
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor with system scope.
     * 
     * @param graph
     */
    public AnnotationViews(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create a link between two items.
     * 
     * @param targetId the identifier of a AccessibleEntity target of this Annotation
     * @param sourceId the identifier of a Annotator source of this Annotation
     * @param bundle the annotation itself
     * @param user
     * @return
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws PermissionDenied
     */
    public Annotation createLink(String targetId, String sourceId, Bundle bundle, Accessor user) throws ItemNotFound, ValidationError,
            PermissionDenied {
        Annotation ann = createFor(targetId, bundle, user);
        ann.setSource(manager.getFrame(sourceId, Annotator.class));
        return ann;
    }

    /**
     * Create an annotation for an item.
     * 
     * @param id the identifier of the AccessibleEntity this annotation is attached to, as a target
     * @param bundle the annotation itself
     * @param user the user creating the annotation
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public Annotation createFor(String id, Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, ItemNotFound {
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        helper.checkEntityPermission(entity, accessor, PermissionType.ANNOTATE);
        Annotation annotation = new BundleDAO(graph).create(bundle,
                Annotation.class);
        graph.frame(entity.asVertex(), AnnotatableEntity.class).addAnnotation(
                annotation);
        annotation.setAnnotator(graph.frame(user.asVertex(),
                Annotator.class));

        new ActionManager(graph).logEvent(entity, graph.frame(user.asVertex(), Actioner.class),
                "Added annotation").addSubjects(annotation);
        return annotation;
    }

    /**
     * Fetch annotations for an item subtree.
     * 
     * @param id
     * @param accessor
     * @return map of ids to annotation lists.
     */
    @Override
    public ListMultimap<String, Annotation> getFor(String id, Accessor accessor)
            throws ItemNotFound {
        final PipeFunction<Vertex, Boolean> filter = acl
                .getAclFilterFunction(accessor);
        final ListMultimap<String, Annotation> annotations = LinkedListMultimap
                .create();
        AnnotatableEntity item = manager.getFrame(id, AnnotatableEntity.class);
        getAnnotations(item, annotations, filter);
        new Serializer(graph).traverseSubtree(item, new TraversalCallback() {
            @Override
            public void process(Frame vertexFrame, int depth,
                    String relation, int relationIndex) {
                getAnnotations(vertexFrame, annotations, filter);
            }
        });
        return annotations;
    }

    /**
     * Fetch annotations for an item and its subtree.
     * 
     * @param item
     * @param annotations
     * @param filter
     */
    private <T extends Frame> void getAnnotations(T item,
            ListMultimap<String, Annotation> annotations,
            PipeFunction<Vertex, Boolean> filter) {
        String id = item.getId();
        AnnotatableEntity entity = graph.frame(item.asVertex(),
                AnnotatableEntity.class);
        for (Annotation ann : entity.getAnnotations()) {
            if (filter.compute(ann.asVertex())) {
                annotations.put(id, ann);
            }
        }
    }
}
