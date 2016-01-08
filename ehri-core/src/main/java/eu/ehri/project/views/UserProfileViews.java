package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;

import java.util.List;


public class UserProfileViews {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final ActionManager actionManager;

    public UserProfileViews(FramedGraph<?> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
        actionManager = new ActionManager(graph);
    }

    public void addWatching(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.addWatching(manager.getEntity(id, Watchable.class));
        }
        log(accessor, ids, EventTypes.watch);
    }

    public void removeWatching(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.removeWatching(manager.getEntity(id, Watchable.class));
        }
        log(accessor, ids, EventTypes.unwatch);
    }

    public void addFollowers(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.addFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(accessor, ids, EventTypes.follow);
    }

    public void removeFollowers(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.removeFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(accessor, ids, EventTypes.unfollow);
    }

    public void addBlocked(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.addBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(accessor, ids, EventTypes.block);
    }

    public void removeBlocked(UserProfile user, List<String> ids, Accessor accessor) throws ItemNotFound {
        for (String id : ids) {
            user.removeBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(accessor, ids, EventTypes.unblock);
    }

    private Optional<SystemEvent> log(Accessor accessor, List<String> ids, EventTypes type)
            throws ItemNotFound {
        if (ids.isEmpty()) {
            return Optional.absent();
        } else {
            ActionManager.EventContext ctx = actionManager
                    .newEventContext(accessor.as(UserProfile.class), type);
            for (String id : ids) {
                ctx.addSubjects(manager.getEntity(id, Accessible.class));
            }
            return Optional.of(ctx.commit());
        }
    }
}
