package eu.ehri.project.views;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.Vertex;
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
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.*;

import java.util.List;

/**
 * View class for handling annotation-related operations.
 *
 * @author mike
 */
public final class LinkViews {

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
    public LinkViews(FramedGraph<?> graph, PermissionScope scope) {
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
    public LinkViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create a link between two items.
     *
     * @param targetId1 the identifier of a AccessibleEntity target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle    the annotation itself
     * @param user
     * @return
     * @throws eu.ehri.project.exceptions.ItemNotFound
     *
     * @throws eu.ehri.project.exceptions.ValidationError
     *
     * @throws eu.ehri.project.exceptions.PermissionDenied
     *
     */
    public Link createLink(String targetId1, String targetId2, List<String> bodies, Bundle bundle,
            Accessor user) throws ItemNotFound,
            ValidationError,
            PermissionDenied {
        LinkableEntity t1 = manager.getFrame(targetId1, LinkableEntity.class);
        LinkableEntity t2 = manager.getFrame(targetId2, LinkableEntity.class);
        helper.checkEntityPermission(t1, user, PermissionType.ANNOTATE);
        helper.checkEntityPermission(t2, user, PermissionType.ANNOTATE);
        Link link = new BundleDAO(graph).create(bundle, Link.class);
        link.addLinkTarget(t1);
        link.addLinkTarget(t2);
        link.setLinker(user);
        for (String body : bodies) {
            AccessibleEntity item = manager.getFrame(body, AccessibleEntity.class);
            link.addLinkBody(item);
        }
        new ActionManager(graph).logEvent(t1, graph.frame(user.asVertex(), Actioner.class),
                "Added link").addSubjects(link).addSubjects(t2);
        return link;
    }
}
