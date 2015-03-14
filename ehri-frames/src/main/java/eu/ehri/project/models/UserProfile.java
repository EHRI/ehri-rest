package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
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

    public static final String FOLLOWER_COUNT = "followers";
    public static final String FOLLOWING_COUNT = "following";
    public static final String WATCHING_COUNT = "watching";

    @Meta(FOLLOWER_COUNT)
    @JavaHandler
    public long getFollowerCount();

    @Meta(FOLLOWING_COUNT)
    @JavaHandler
    public long getFollowingCount();

    @Meta(WATCHING_COUNT)
    @JavaHandler
    public long getWatchingCount();

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
        public void addFollowing(final UserProfile user) {
            addSingleRelationship(it(), user.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public void removeFollowing(final UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public boolean isFollowing(final UserProfile otherUser) {
            return hasRelationship(it(), otherUser.asVertex(), USER_FOLLOWS_USER);
        }

        @Override
        public boolean isFollower(final UserProfile otherUser) {
            return hasRelationship(otherUser.asVertex(), it(), USER_FOLLOWS_USER);
        }

        @Override
        public void addWatching(final Watchable item) {
            addSingleRelationship(it(), item.asVertex(), USER_WATCHING_ITEM);
        }

        @Override
        public void removeWatching(final Watchable item) {
            removeAllRelationships(it(), item.asVertex(), USER_WATCHING_ITEM);
        }

        @Override
        public boolean isWatching(final Watchable item) {
            return hasRelationship(it(), item.asVertex(), USER_WATCHING_ITEM);
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
