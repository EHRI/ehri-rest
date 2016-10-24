package eu.ehri.project.api.impl;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.api.Api;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.api.UserProfilesApi;

import java.util.List;
import java.util.Optional;


class UserProfilesApiImpl implements UserProfilesApi {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Api api;

    UserProfilesApiImpl(FramedGraph<?> graph, Api api) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.api = api;
    }

    @Override
    public UserProfile addWatching(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.addWatching(manager.getEntity(id, Watchable.class));
        }
        log(user, ids, EventTypes.watch);
        return user;
    }

    @Override
    public UserProfile removeWatching(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.removeWatching(manager.getEntity(id, Watchable.class));
        }
        log(user, ids, EventTypes.unwatch);
        return user;
    }

    @Override
    public UserProfile addFollowers(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.addFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.follow);
        return user;
    }

    @Override
    public UserProfile removeFollowers(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.removeFollowing(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.unfollow);
        return user;
    }

    @Override
    public UserProfile addBlocked(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.addBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.block);
        return user;
    }

    @Override
    public UserProfile removeBlocked(String userId, List<String> ids) throws ItemNotFound {
        UserProfile user = api.detail(userId, UserProfile.class);
        for (String id : ids) {
            user.removeBlocked(manager.getEntity(id, UserProfile.class));
        }
        log(user, ids, EventTypes.unblock);
        return user;
    }

    private Optional<SystemEvent> log(UserProfile user, List<String> ids, EventTypes type)
            throws ItemNotFound {
        if (api.isLogging() && !ids.isEmpty()) {
            ActionManager.EventContext ctx = api.actionManager()
                    .newEventContext(user, api.accessor().as(Actioner.class), type);
            for (String id : ids) {
                ctx.addSubjects(manager.getEntity(id, Accessible.class));
            }
            return Optional.of(ctx.commit());
        } else {
            return Optional.empty();
        }
    }
}
