package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;


public class AclEdge extends AclElement implements Edge {

    protected AclEdge(Edge baseEdge, AclGraph<?> aclGraph) {
        super(baseEdge, aclGraph);
    }

    @Override
    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
        Vertex vertex = ((Edge) baseElement).getVertex(direction);
        return aclGraph.evaluateVertex(vertex) ? new AclVertex(((Edge) baseElement).getVertex(direction),
                aclGraph) : null;
    }

    @Override
    public String getLabel() {
        return ((Edge) this.baseElement).getLabel();
    }

    public Edge getBaseEdge() {
        return (Edge) this.baseElement;
    }
}
