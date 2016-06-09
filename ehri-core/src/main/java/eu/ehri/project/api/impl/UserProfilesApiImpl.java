package eu.ehri.project.api.impl;

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.api.UserProfilesApi;

import java.util.List;


class UserProfilesApiImpl implements UserProfilesApi {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final ActionManager actionManager;
    private final Accessor accessor;
    private final boolean logging;

    UserProfilesApiImpl(FramedGraph<?> graph, Accessor accessor, boolean logging) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
        actionManager = new ActionManager(graph);
        this.accessor = accessor;
        this.logging = logging;
    }

    @Override
    public void addWatching(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.addWatching(manager.getEntity(id, Watchable.class));
        }
        log(user, ids, EventTypes.watch);
    }

    @Override
    public void removeWatching(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.removeWatching(manager.getEntity(id, Watchable.class));
        }
        log(user, ids, EventTypes.unwatch);
    }

    @Override
    public void addFollowers(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.addFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.follow);
    }

    @Override
    public void removeFollowers(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.removeFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.unfollow);
    }

    @Override
    public void addBlocked(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.addBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.block);
    }

    @Override
    public void removeBlocked(UserProfile user, List<String> ids) throws ItemNotFound {
        for (String id : ids) {
            user.removeBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.unblock);
    }

    private Optional<SystemEvent> log(UserProfile user, List<String> ids, EventTypes type)
            throws ItemNotFound {
        if (logging && !ids.isEmpty()) {
            ActionManager.EventContext ctx = actionManager
                    .newEventContext(user, accessor.as(Actioner.class), type);
            for (String id : ids) {
                ctx.addSubjects(manager.getEntity(id, Accessible.class));
            }
            return Optional.of(ctx.commit());
        } else {
            return Optional.absent();
        }
    }
}
