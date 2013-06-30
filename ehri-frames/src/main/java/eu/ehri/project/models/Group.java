package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;

import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerImpl;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.GROUP)
public interface Group extends Accessor, AccessibleEntity, IdentifiableEntity,
        PermissionScope, NamedEntity {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";
    String ADMIN_GROUP_NAME = "Administrators";

    @Fetch(BELONGS_TO)
    @Adjacency(label = BELONGS_TO)
    public Iterable<Group> getGroups();

    /**
     * TODO FIXME use this in case we need AccesibleEnity's instead of Accessors, 
     */
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public Iterable<AccessibleEntity> getMembersAsEntities();

    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public Iterable<Accessor> getMembers();

    /**
     * adds a Accessor as a member to this Group, so it has the permissions of the Group.
     * @param accessor 
     */
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public void addMember(final Accessor accessor);
    
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public void removeMember(final Accessor accessor);

    // FIXME: Use of __ISA__ here breaks encapsulation of indexing details quite horribly
    @JavaHandler
    public Iterable<UserProfile> getAllUserProfileMembers();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerImpl<Vertex>, Group {
        public Iterable<UserProfile> getAllUserProfileMembers() {
            GremlinPipeline<Vertex,Vertex> pipe = gremlin().as("n").in(BELONGS_TO)
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
