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
public class PromotionViews {

    private final ViewHelper helper;
    private final ActionManager actionManager;

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
     * Add a promotion to an item.
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void promoteItem(Promotable item, UserProfile user) throws PermissionDenied,
            NotPromotableError {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        item.addPromotion(user);
        actionManager.logEvent(item, user, EventTypes.promotion);
    }

    /**
     * Remove a promotion from an item
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void demoteItem(Promotable item, UserProfile user) throws PermissionDenied {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        // Should we complain here if the item is not promotable?
        // I think probably not, since this is then correcting an error...
        item.removePromotion(user);
        actionManager.logEvent(item, user, EventTypes.demotion);
    }
}
