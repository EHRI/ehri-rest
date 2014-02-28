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
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.GROUP)
public interface Group extends Accessor, AccessibleEntity, IdentifiableEntity,
        PermissionScope, NamedEntity, ItemHolder {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";
    String ADMIN_GROUP_NAME = "Administrators";

    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Group> getGroups();

    /**
     * TODO FIXME use this in case we need AccesibleEnity's instead of Accessors, 
     */
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<AccessibleEntity> getMembersAsEntities();

    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<Accessor> getMembers();

    @JavaHandler
    public Long getChildCount();

    @JavaHandler
    public void updateChildCountCache();


    /**
     * adds a Accessor as a member to this Group, so it has the permissions of the Group.
     */
    @JavaHandler
    public void addMember(final Accessor accessor);
    
    @JavaHandler
    public void removeMember(final Accessor accessor);

    // FIXME: Use of __ISA__ here breaks encapsulation of indexing details quite horribly
    // FIXME: Should return an Accessor here, hierarchies are confusing.
    @JavaHandler
    public Iterable<AccessibleEntity> getAllUserProfileMembers();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Group {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.ACCESSOR_BELONGS_TO_GROUP).count());
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.ACCESSOR_BELONGS_TO_GROUP).count();
            }
            return count;
        }

        public void addMember(final Accessor accessor) {
            accessor.asVertex().addEdge(Ontology.ACCESSOR_BELONGS_TO_GROUP, it());
            updateChildCountCache();
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
