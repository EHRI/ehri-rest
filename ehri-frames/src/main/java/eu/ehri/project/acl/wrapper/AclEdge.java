package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclEdge extends AclElement implements Edge {
    protected AclEdge(final Edge baseEdge, final AclGraph<?> aclGraph) {
        super(baseEdge, aclGraph);
    }

    @Override
    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
        final Vertex vertex = ((Edge) baseElement).getVertex(direction);
        return graph.test(vertex)
                ? new AclVertex(((Edge) baseElement).getVertex(direction), graph)
                : null;
    }

    @Override
    public String getLabel() {
        return ((Edge) this.baseElement).getLabel();
    }

    public Edge getBaseEdge() {
        return (Edge) this.baseElement;
    }
}
