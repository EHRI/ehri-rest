package eu.ehri.project.views;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.persistence.ActionManager;

/**
 * View class for managing item promotion.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PromotionViews implements Scoped<PromotionViews> {

    private final ViewHelper helper;
    private final ActionManager actionManager;
    private final FramedGraph<?> graph;

    public static class NotPromotableError extends Exception {
        public NotPromotableError(String itemId) {
            super(String.format("Item '%s' is not marked as promotable.", itemId));
        }
    }

    /**
     * Scoped constructor.
     *
     * @param graph A framed graph
     * @param scope An item scope
     */
    public PromotionViews(FramedGraph<?> graph, PermissionScope scope) {
        this.graph = graph;
        helper = new ViewHelper(graph, scope);
        actionManager = new ActionManager(graph, scope);
    }

    /**
     * Constructor with system scope.
     *
     * @param graph A framed graph
     */
    public PromotionViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Up vote an item, removing a down vote if there is one.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void upVote(Promotable item, UserProfile user) throws PermissionDenied,
            NotPromotableError {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        item.upVote(user);
        actionManager.logEvent(item, user, EventTypes.promotion);
    }

    /**
     * Remove an up vote.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void removeUpVote(Promotable item, UserProfile user) throws PermissionDenied {
        item.removeUpVote(user);
    }

    /**
     * Down vote an item, removing an up vote if there is one.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void downVote(Promotable item, UserProfile user) throws PermissionDenied,
            NotPromotableError {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        item.downVote(user);
        actionManager.logEvent(item, user, EventTypes.demotion);
    }

    /**
     * Remove a down vote.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void removeDownVote(Promotable item, UserProfile user) throws PermissionDenied {
        item.removeDownVote(user);
    }

    @Override
    public PromotionViews withScope(PermissionScope scope) {
        return new PromotionViews(graph, scope);
    }
}
