package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.AccessDenied;
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
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class AnnotationViews implements Scoped<AnnotationViews> {

    private final FramedGraph<?> graph;
    private final AclManager acl;
    private final ViewHelper helper;
    private final GraphManager manager;

    /**
     * Scoped constructor.
     *
     * @param graph The graph
     * @param scope The view scope
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
     * @param graph The graph
     */
    public AnnotationViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create an annotation for a dependent node of an item. The entity and the
     * dependent item can be the same.
     *
     * @param id           the identifier of the AccessibleEntity this annotation is attached to, as a target
     * @param did          the identifier of the dependent item
     * @param bundle       the annotation itself
     * @param user         the user creating the annotation
     * @param accessibleTo Users or groups who can access this annotation
     * @return the created annotation
     * @throws PermissionDenied
     * @throws AccessDenied
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public Annotation createFor(String id, String did, Bundle bundle, Accessor user, Iterable<Accessor> accessibleTo)
            throws PermissionDenied, AccessDenied, ValidationError, ItemNotFound {
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AnnotatableEntity dep = manager.getFrame(did, AnnotatableEntity.class);
        helper.checkEntityPermission(entity, user, PermissionType.ANNOTATE);
        helper.checkReadAccess(entity, user);

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
        acl.setAccessors(annotation, accessibleTo);
        acl.withScope(SystemScope.INSTANCE)
                .grantPermission(annotation, PermissionType.OWNER, user);

        new ActionManager(graph, entity).logEvent(annotation,
                graph.frame(user.asVertex(), Actioner.class),
                EventTypes.annotation, Optional.<String>absent());
        return annotation;
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

    @Override
    public AnnotationViews withScope(PermissionScope scope) {
        return new AnnotationViews(graph, scope);
    }
}
