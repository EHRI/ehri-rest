package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.partition.PartitionGraph;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclEdge extends AclElement implements Edge {
    private final PipeFunction<Vertex,Boolean> aclFilter;

    protected AclEdge(final Edge baseEdge, final AclGraph<?> aclGraph) {
        super(baseEdge, aclGraph);
        aclFilter = AclManager.getAclFilterFunction(aclGraph.getAccessor());
    }

    @Override
    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
        final Vertex vertex = ((Edge) baseElement).getVertex(direction);
        return aclFilter.compute(vertex) ? new AclVertex(((Edge) baseElement).getVertex(direction), graph) : null;
    }

    @Override
    public String getLabel() {
        return ((Edge) this.baseElement).getLabel();
    }

    public Edge getBaseEdge() {
        return (Edge) this.baseElement;
    }
}
