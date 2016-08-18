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
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Versioned;
import eu.ehri.project.models.base.Watchable;

import static eu.ehri.project.definitions.Ontology.ACCESSOR_BELONGS_TO_GROUP;
import static eu.ehri.project.definitions.Ontology.USER_FOLLOWS_USER;
import static eu.ehri.project.definitions.Ontology.USER_WATCHING_ITEM;
import static eu.ehri.project.models.utils.JavaHandlerUtils.*;

/**
 * A frame class representing a user within the database.
 */
@EntityType(EntityClass.USER_PROFILE)
public interface UserProfile extends Accessor, Actioner, Versioned, Annotatable {

    String FOLLOWER_COUNT = "followers";
    String FOLLOWING_COUNT = "following";
    String WATCHING_COUNT = "watching";

    @Meta(FOLLOWER_COUNT)
    @JavaHandler
    long getFollowerCount();

    @Meta(FOLLOWING_COUNT)
    @JavaHandler
    long getFollowingCount();

    @Meta(WATCHING_COUNT)
    @JavaHandler
    long getWatchingCount();

    /**
     * Get the groups to which this user belongs.
     *
     * @return an iterable of group frames
     */
    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = ACCESSOR_BELONGS_TO_GROUP)
    Iterable<Group> getGroups();

    /**
     * Get users who follow this user.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = USER_FOLLOWS_USER, direction = Direction.IN)
    Iterable<UserProfile> getFollowers();

    /**
     * Get users who this user follows.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = USER_FOLLOWS_USER, direction = Direction.OUT)
    Iterable<UserProfile> getFollowing();

    /**
     * Add a user the those this user follows.
     *
     * @param user a user frame
     */
    @JavaHandler
    void addFollowing(UserProfile user);

    /**
     * Remove a user from those this user follows.
     *
     * @param user a user frame
     */
    @JavaHandler
    void removeFollowing(UserProfile user);

    /**
     * Ascertain whether this user is following another user.
     *
     * @param otherUser a user frame
     * @return whether this user is following the other
     */
    @JavaHandler
    boolean isFollowing(UserProfile otherUser);

    /**
     * Ascertain whether the other user is following this user.
     *
     * @param otherUser a user frame
     * @return whether the other user is following this user
     */
    @JavaHandler
    boolean isFollower(UserProfile otherUser);

    /**
     * Fetch items this user is watching.
     *
     * @return an iterable of generic item frames
     */
    @Adjacency(label = USER_WATCHING_ITEM, direction = Direction.OUT)
    Iterable<Watchable> getWatching();

    /**
     * Fetch links belonging to this user.
     *
     * @return an iterable of link frames
     */
    @Adjacency(label = Ontology.LINK_HAS_LINKER, direction = Direction.IN)
    Iterable<Link> getLinks();

    /**
     * Fetch annotations belonging to this user.
     *
     * @return an iterable of annotation frames
     */
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION)
    Iterable<Annotation> getAnnotations();

    /**
     * Fetch virtual units created by this user.
     *
     * @return an iterable of virtual unit frames
     */
    @Adjacency(label = Ontology.VC_HAS_AUTHOR, direction = Direction.IN)
    Iterable<VirtualUnit> getVirtualUnits();

    /**
     * Add an item to this user's watch list.
     *
     * @param item a generic item frame
     */
    @JavaHandler
    void addWatching(Watchable item);

    /**
     * Remove an item from this user's watch list.
     *
     * @param item a generic item frame
     */
    @JavaHandler
    void removeWatching(Watchable item);

    /**
     * Add a user to this user's block list.
     *
     * @param user a user frame
     */
    @JavaHandler
    void addBlocked(UserProfile user);

    /**
     * Remove a user from this user's block list.
     *
     * @param user a user frame
     */
    @JavaHandler
    void removeBlocked(UserProfile user);

    /**
     * Fetch users on this user's block list.
     *
     * @return an iterable of user frames
     */
    @Adjacency(label = Ontology.USER_BLOCKS_USER)
    Iterable<UserProfile> getBlocked();

    /**
     * Ascertain whether this user is blocking another user.
     *
     * @param user a user frame
     * @return whther the other user is on this user's block list
     */
    @JavaHandler
    boolean isBlocking(UserProfile user);

    /**
     * Fetch users who share groups with this user.
     *
     * @return an iterable of user frames
     */
    @JavaHandler
    Iterable<UserProfile> coGroupMembers();

    /**
     * Ascertain whether this user is watching an item.
     *
     * @param item a generic item frame
     * @return whether the item is in this user's watch list
     */
    @JavaHandler
    boolean isWatching(Watchable item);


    abstract class Impl implements JavaHandlerContext<Vertex>, UserProfile {

        @Override
        public long getFollowerCount() {
            return gremlin().inE(USER_FOLLOWS_USER).count();
        }

        @Override
        public long getFollowingCount() {
            return gremlin().outE(USER_FOLLOWS_USER).count();
        }

        @Override
        public long getWatchingCount() {
            return gremlin().outE(USER_WATCHING_ITEM).count();
        }

        @Override
        public void addFollowing(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public void removeFollowing(UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public boolean isFollowing(UserProfile otherUser) {
            return hasRelationship(it(), otherUser.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public boolean isFollower(UserProfile otherUser) {
            return hasRelationship(otherUser.asVertex(), it(), USER_FOLLOWS_USER);
        }

        @Override
        public void addWatching(Watchable item) {
            addUniqueRelationship(it(), item.asVertex(), USER_WATCHING_ITEM);
        }

        @Override
        public void removeWatching(Watchable item) {
            removeAllRelationships(it(), item.asVertex(), USER_WATCHING_ITEM);
        }

        @Override
        public boolean isWatching(Watchable item) {
            return hasRelationship(it(), item.asVertex(), USER_WATCHING_ITEM);
        }

        @Override
        public Iterable<UserProfile> coGroupMembers() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", defaultMaxLoops, noopLoopFunc)
                    .in(Ontology.ACCESSOR_BELONGS_TO_GROUP).filter(vertex -> {
                        // Exclude the current user...
                        if (it().equals(vertex)) {
                            return false;
                        }
                        // Exclude other groups...
                        String type = vertex.getProperty(EntityType.TYPE_KEY);
                        return Entities.USER_PROFILE.equals(type);
                    }));
        }

        @Override
        public void addBlocked(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }

        @Override
        public void removeBlocked(UserProfile user) {
            removeAllRelationships(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }

        @Override
        public boolean isBlocking(UserProfile user) {
            return hasRelationship(it(), user.asVertex(),
                    Ontology.USER_BLOCKS_USER);
        }
    }
}
