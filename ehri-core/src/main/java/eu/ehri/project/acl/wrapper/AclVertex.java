package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperVertexQuery;


public class AclVertex extends AclElement implements Vertex {

    protected AclVertex(Vertex baseVertex, AclGraph<?> graph) {
        super(baseVertex, graph);
    }

    @Override
    public Iterable<Edge> getEdges(Direction direction, String... strings) {
        return new AclEdgeIterable(((Vertex) this.baseElement).getEdges(direction, strings), this.graph);
    }

    @Override
    public Iterable<Vertex> getVertices(Direction direction, String... strings) {
        return new AclVertexIterable(((Vertex) this.baseElement).getVertices(direction, strings), this.graph);
    }

    @Override
    public VertexQuery query() {
        return new WrapperVertexQuery(((Vertex) this.baseElement).query()) {
            @Override
            public Iterable<Vertex> vertices() {
                return new AclVertexIterable(this.query.vertices(), graph);
            }

            @Override
            public Iterable<Edge> edges() {
                return this.query.edges();
            }
        };
    }

    @Override
    public Edge addEdge(String label, Vertex vertex) {
        return this.graph.addEdge(null, this, vertex, label);
    }

    public Vertex getBaseVertex() {
        return (Vertex) this.baseElement;
    }
}
