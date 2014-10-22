package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.NamedEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing a group of users or other groups
 * that can be assigned permissions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.GROUP)
public interface Group extends Accessor, AccessibleEntity, IdentifiableEntity,
        PermissionScope, NamedEntity, ItemHolder {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";
    String ADMIN_GROUP_NAME = "Administrators";

    /**
     * Fetch the groups to which this group belongs.
     *
     * @return an iterable of group frames
     */
    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Group> getGroups();

    /**
     * TODO FIXME use this in case we need AccesibleEnity's instead of Accessors, 
     */
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<AccessibleEntity> getMembersAsEntities();

    /**
     * Get members of this group.
     *
     * @return an iterable of user or group frames
     */
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<Accessor> getMembers();

    /**
     * Get the number of items within this group.
     *
     * @return a count of group members
     */
    @JavaHandler
    public long getChildCount();

    /**
     * Update/reset the cache of the number of items
     * in this group.
     */
    @JavaHandler
    public void updateChildCountCache();


    /**
     * Adds a Accessor as a member to this Group, so it has the permissions of the Group.
     *
     * @param accessor a user or group frame
     */
    @JavaHandler
    public void addMember(final Accessor accessor);

    /**
     * Removes a member from this group.
     *
     * @param accessor a user or group frame
     */
    @JavaHandler
    public void removeMember(final Accessor accessor);

    /**
     * Get <b>all</b> members of this group, including members of
     * groups within this group.
     *
     * @return an iterable of user or group frames
     */
    @JavaHandler
    public Iterable<AccessibleEntity> getAllUserProfileMembers();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Group {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.ACCESSOR_BELONGS_TO_GROUP).count());
        }

        public long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.ACCESSOR_BELONGS_TO_GROUP).count();
            }
            return count;
        }

        public void addMember(final Accessor accessor) {
            if (JavaHandlerUtils.addUniqueRelationship(accessor.asVertex(), it(),
                    Ontology.ACCESSOR_BELONGS_TO_GROUP)) {
                updateChildCountCache();
            }
        }

        public void removeMember(final Accessor accessor) {
            for (Edge e : it().getEdges(Direction.IN, Ontology.ACCESSOR_BELONGS_TO_GROUP)) {
                if (e.getVertex(Direction.OUT).equals(accessor.asVertex())) {
                    e.remove();
                    break;
                }
            }
            updateChildCountCache();
        }

        public Iterable<AccessibleEntity> getAllUserProfileMembers() {
            GremlinPipeline<Vertex,Vertex> pipe = gremlin().as("n").in(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return vertexLoopBundle.getObject()
                                    .getProperty(EntityType.TYPE_KEY)
                                    .equals(Entities.USER_PROFILE);
                        }
                    });
            return frameVertices(pipe.dedup());
        }
    }
}
