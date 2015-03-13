/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of 
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */
package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.NamedEntity;
import eu.ehri.project.models.base.Watchable;

import static eu.ehri.project.definitions.Ontology.ACCESSOR_BELONGS_TO_GROUP;
import static eu.ehri.project.definitions.Ontology.USER_FOLLOWS_USER;
import static eu.ehri.project.definitions.Ontology.USER_WATCHING_ITEM;
import static eu.ehri.project.models.utils.JavaHandlerUtils.*;

/**
 * A frame class representing a user within the database.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.USER_PROFILE)
public interface UserProfile extends Accessor, AccessibleEntity, IdentifiableEntity, 
        Annotator, Actioner, NamedEntity {

    public static String FOLLOWER_COUNT = "_followers";
    public static String FOLLOWING_COUNT = "_following";
    public static String WATCHING_COUNT = "_watching";
    public static String WATCHED_COUNT = "_watchedBy";

    /**
     * Get the groups to which this user belongs.
     *
     * @return an iterable of group frames
     */
    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Group> getGroups();

    /**
     * Get users who follow this user.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = USER_FOLLOWS_USER, direction = Direction.IN)
    public Iterable<UserProfile> getFollowers();

    /**
     * Get users who this user follows.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = USER_FOLLOWS_USER, direction = Direction.OUT)
    public Iterable<UserProfile> getFollowing();

    /**
     * Add a user the those this user follows.
     *
     * @param user a user frame
     */
    @JavaHandler
    public void addFollowing(final UserProfile user);

    /**
     * Remove a user from those this user follows.
     *
     * @param user a user frame
     */
    @JavaHandler
    public void removeFollowing(final UserProfile user);

    /**
     * Ascertain whether this user is following another user.
     *
     * @param otherUser a user frame
     * @return whether this user is following the other
     */
    @JavaHandler
    public boolean isFollowing(final UserProfile otherUser);

    /**
     * Ascertain whether the other user is following this user.
     *
     * @param otherUser a user frame
     * @return whether the other user is following this user
     */
    @JavaHandler
    public boolean isFollower(final UserProfile otherUser);

    /**
     * Fetch items this user is watching.
     *
     * @return an iterable of generic item frames
     */
    @Adjacency(label = USER_WATCHING_ITEM, direction = Direction.OUT)
    public Iterable<Watchable> getWatching();

    /**
     * Fetch links belonging to this user.
     *
     * @return an iterable of link frames
     */
    @Adjacency(label = Ontology.LINK_HAS_LINKER, direction = Direction.IN)
    public Iterable<Link> getLinks();

    /**
     * Fetch annotations belonging to this user.
     *
     * @return an iterable of annotation frames
     */
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION)
    @Override
    public Iterable<Annotation> getAnnotations();

    /**
     * Fetch virtual units created by this user.
     *
     * @return an iterable of virtual unit frames
     */
    @Adjacency(label = Ontology.VC_HAS_AUTHOR, direction = Direction.IN)
    public Iterable<VirtualUnit> getVirtualUnits();

    /**
     * Add an item to this user's watch list.
     *
     * @param item a generic item frame
     */
    @JavaHandler
    public void addWatching(final Watchable item);

    /**
     * Remove an item from this user's watch list.
     *
     * @param item a generic item frame
     */
    @JavaHandler
    public void removeWatching(final Watchable item);

    /**
     * Add a user to this user's block list.
     *
     * @param user a user frame
     */
    @JavaHandler
    public void addBlocked(final UserProfile user);

    /**
     * Remove a user from this user's block list.
     *
     * @param user a user frame
     */
    @JavaHandler
    public void removeBlocked(final UserProfile user);

    /**
     * Fetch users on this user's block list.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = Ontology.USER_BLOCKS_USER)
    public Iterable<UserProfile> getBlocked();

    /**
     * Ascertain whether this user is blocking another user.
     * @param user a user frame
     * @return whther the other user is on this user's block list
     */
    @JavaHandler
    public boolean isBlocking(final UserProfile user);

    /**
     * Fetch users who share groups with this user.
     *
     * @return an iterable of user frames
     */
    @JavaHandler
    public Iterable<UserProfile> coGroupMembers();

    /**
     * Ascertain whether this user is watching an item.
     *
     * @param item a generic item frame
     * @return whether the item is in this user's watch list
     */
    @JavaHandler
    public boolean isWatching(final Watchable item);


    abstract class Impl implements JavaHandlerContext<Vertex>, UserProfile {

        private void updateFollowCounts(Vertex user, Vertex other) {
            cacheCount(
                    user, gremlin().out(USER_FOLLOWS_USER), FOLLOWING_COUNT);
            cacheCount(
                    other,
                    gremlin().start(other).in(USER_FOLLOWS_USER), FOLLOWER_COUNT);
        }

        private void updateWatchCount(Vertex user, Vertex item) {
            cacheCount(
                    user, gremlin().out(USER_WATCHING_ITEM), WATCHING_COUNT);
            cacheCount(
                    item,
                    gremlin().start(item).in(USER_WATCHING_ITEM), WATCHED_COUNT);
        }

        @Override
        public void addFollowing(final UserProfile user) {
            if (!isFollowing(user)) {
                it().addEdge(USER_FOLLOWS_USER, user.asVertex());
                updateFollowCounts(it(), user.asVertex());
            }
        }

        @Override
        public void removeFollowing(final UserProfile user) {
            for (Edge e : it().getEdges(Direction.OUT, USER_FOLLOWS_USER)) {
                if (e.getVertex(Direction.IN).equals(user.asVertex())) {
                    e.remove();
                }
            }
            updateFollowCounts(it(), user.asVertex());
        }

        @Override
        public boolean isFollowing(final UserProfile otherUser) {
            return gremlin().out(USER_FOLLOWS_USER).filter(new PipeFunction<Vertex, Boolean>() {
                @Override
                public Boolean compute(Vertex vertex) {
                    return vertex.equals(otherUser.asVertex());
                }
            }).hasNext();
        }

        @Override
        public boolean isFollower(final UserProfile otherUser) {
            return gremlin().in(USER_FOLLOWS_USER).filter(new PipeFunction<Vertex, Boolean>() {
                @Override
                public Boolean compute(Vertex vertex) {
                    return vertex.equals(otherUser.asVertex());
                }
            }).hasNext();
        }

        @Override
        public void addWatching(final Watchable item) {
            if (!isWatching(item)) {
                it().addEdge(USER_WATCHING_ITEM, item.asVertex());
                updateWatchCount(it(), item.asVertex());
            }
        }

        @Override
        public void removeWatching(final Watchable item) {
            for (Edge e : it().getEdges(Direction.OUT, USER_WATCHING_ITEM)) {
                if (e.getVertex(Direction.IN).equals(item.asVertex())) {
                    e.remove();
                }
            }
            updateWatchCount(it(), item.asVertex());
        }

        @Override
        public boolean isWatching(final Watchable item) {
            return gremlin().out(USER_WATCHING_ITEM).filter(new PipeFunction<Vertex, Boolean>() {
                @Override
                public Boolean compute(Vertex vertex) {
                    return vertex.equals(item.asVertex());
                }
            }).hasNext();
        }

        @Override
        public Iterable<UserProfile> coGroupMembers() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", defaultMaxLoops, noopLoopFunc)
                    .in(Ontology.ACCESSOR_BELONGS_TO_GROUP).filter(new PipeFunction<Vertex, Boolean>() {
                        @Override
                        public Boolean compute(Vertex vertex) {
                            // Exclude the current user...
                            if (it().equals(vertex)) {
                                return false;
                            }
                            // Exclude other groups...
                            String type = vertex.getProperty(EntityType.TYPE_KEY);
                            return !(type == null || !type.equals(Entities.USER_PROFILE));
                        }
                    }));
        }

        @Override
        public void addBlocked(final UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }

        @Override
        public void removeBlocked(final UserProfile user) {
            removeAllRelationships(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }

        @Override
        public boolean isBlocking(final UserProfile user) {
            return hasRelationship(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }
    }
}
