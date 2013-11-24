package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.*;

import java.util.Set;

/**
 * View class for handling annotation-related operations.
 * 
 * @author mike
 */
public final class AnnotationViews {

    private final FramedGraph<?> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final GraphManager manager;

    /**
     * Scoped constructor.
     * 
     * @param graph
     * @param scope
     */
    public AnnotationViews(FramedGraph<?> graph, PermissionScope scope) {
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
    public AnnotationViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create an annotation for a dependent node of an item. The entity and the
     * dependent item can be the same.
     *
     * @param id the identifier of the AccessibleEntity this annotation is attached to, as a target
     * @param did the identifier of the dependent item
     * @param bundle the annotation itself
     * @param user the user creating the annotation
     * @return the created annotation
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public Annotation createFor(String id, String did, Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, ItemNotFound {
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AnnotatableEntity dep = manager.getFrame(did, AnnotatableEntity.class);
        helper.checkEntityPermission(entity, user, PermissionType.ANNOTATE);

        if (!(entity.equals(dep) || isInSubtree(entity, dep))) {
            // FIXME: Better error message here...
            throw new PermissionDenied("Item is not covered by parent item's permissions");
        }


        Annotation annotation = new BundleDAO(graph).create(bundle, Annotation.class);
        entity.addAnnotation(annotation);
        if (!entity.equals(dep)) {
            dep.addAnnotationPart(annotation);
        }
        annotation.setAnnotator(graph.frame(user.asVertex(), Annotator.class));

        new ActionManager(graph, entity).logEvent(annotation,
                graph.frame(user.asVertex(), Actioner.class),
                EventTypes.annotation, Optional.<String>absent());
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

    private boolean isInSubtree(Frame parent, Frame child) {
        // Check dependent is within item's subtree!
        final Set<String> deps = Sets.newHashSet();
        new Serializer(graph).traverseSubtree(parent, new TraversalCallback() {
            @Override
            public void process(Frame vertexFrame, int depth,
                    String relation, int relationIndex) {
                deps.add(vertexFrame.getId());
            }
        });
        return deps.contains(child.getId());
    }
}
