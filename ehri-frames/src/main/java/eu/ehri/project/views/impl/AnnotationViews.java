package eu.ehri.project.views.impl;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;
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
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.PermissionScope;
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
     * @param cls
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
     * @param cls
     */
    public AnnotationViews(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create a link between two items.
     * 
     * @param id
     * @param sourceId
     * @param bundle
     * @param user
     * @return
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws PermissionDenied
     */
    public Annotation createLink(String id, String sourceId, Bundle bundle,
            Accessor user) throws ItemNotFound, ValidationError,
            PermissionDenied {
        Annotation ann = createFor(id, bundle, user);
        ann.setSource(manager.getFrame(sourceId, Annotator.class));
        return ann;
    }

    /**
     * Create an annotation for an item.
     * 
     * @param id
     * @param bundle
     * @param accessor
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public Annotation createFor(String id, Bundle bundle, Accessor accessor)
            throws PermissionDenied, ValidationError, ItemNotFound {
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        helper.checkEntityPermission(entity, accessor, PermissionType.ANNOTATE);
        // FIXME: This kind of sucks, generating a UUID identifier
        // manually - we should relax the restriction to have one.
        if (bundle.getDataValue(AccessibleEntity.IDENTIFIER_KEY) == null) {
            bundle = bundle.withDataValue(AccessibleEntity.IDENTIFIER_KEY,
                    java.util.UUID.randomUUID().toString());
        }
        Annotation annotation = new BundleDAO(graph).create(bundle,
                Annotation.class);
        graph.frame(entity.asVertex(), AnnotatableEntity.class).addAnnotation(
                annotation);
        annotation.setAnnotator(graph.frame(accessor.asVertex(),
                Annotator.class));

        new ActionManager(graph).createAction(entity, accessor,
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
    public ListMultimap<String, Annotation> getFor(String id, Accessor accessor)
            throws ItemNotFound {
        final PipeFunction<Vertex, Boolean> filter = acl
                .getAclFilterFunction(accessor);
        final ListMultimap<String, Annotation> annotations = LinkedListMultimap
                .create();
        AnnotatableEntity item = manager.getFrame(id, AnnotatableEntity.class);
        getAnnotations(item, annotations, filter);
        new Serializer(graph).traverseSubtree(item, new TraversalCallback() {
            public void process(VertexFrame vertexFrame, int depth,
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
     * @param depth
     * @param annotations
     * @param filter
     */
    private <T extends VertexFrame> void getAnnotations(T item,
            ListMultimap<String, Annotation> annotations,
            PipeFunction<Vertex, Boolean> filter) {
        String id = manager.getId(item);
        AnnotatableEntity entity = graph.frame(item.asVertex(),
                AnnotatableEntity.class);
        for (Annotation ann : entity.getAnnotations()) {
            if (filter.compute(ann.asVertex())) {
                annotations.put(id, ann);
            }
        }
    }
}
