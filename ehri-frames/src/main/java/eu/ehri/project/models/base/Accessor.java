package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerImpl;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.models.PermissionGrant;

public interface Accessor extends IdentifiableEntity {
    public static final String BELONGS_TO = "belongsTo";

    abstract class Impl implements JavaHandlerImpl<Vertex>, Accessor {
        public Iterable<Accessor> getAllParents() {
            return frameVertices(gremlin().as("n")
                    .out(BELONGS_TO)
                    .loop("n", new PipeFunction<LoopPipe.LoopBundle<Vertex>,
                            Boolean>() {
                @Override
                public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                    return vertexLoopBundle.getLoops() < 20;
                }
            }, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                @Override
                public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                    return true;
                }
            }));
        }
    }

    @Adjacency(label = BELONGS_TO)
    public Iterable<Accessor> getParents();

    //@GremlinGroovy("it.as('n').out('" + BELONGS_TO
    //        + "').loop('n'){it.loops < 20}{true}")
    @JavaHandler
    public Iterable<Accessor> getAllParents();

    @Adjacency(label = PermissionGrant.HAS_SUBJECT, direction=Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();

    @Adjacency(label = PermissionGrant.HAS_SUBJECT, direction=Direction.IN)
    public void addPermissionGrant(final PermissionGrant grant);
}
