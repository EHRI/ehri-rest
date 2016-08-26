package eu.ehri.project.core.impl.neo4j;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;
import com.tinkerpop.blueprints.util.DefaultVertexQuery;
import com.tinkerpop.blueprints.util.MultiIterable;
import com.tinkerpop.blueprints.util.StringFactory;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


public class Neo4j2Vertex extends Neo4j2Element implements Vertex {

    public Neo4j2Vertex(Node node, Neo4j2Graph graph) {
        super(graph);
        this.rawElement = node;

    }

    public Iterable<Edge> getEdges(com.tinkerpop.blueprints.Direction direction, String... labels) {
        this.graph.autoStartTransaction(false);
        if (direction.equals(com.tinkerpop.blueprints.Direction.OUT))
            return new Neo4jVertexEdgeIterable(this.graph, (Node) this.rawElement, Direction.OUTGOING, labels);
        else if (direction.equals(com.tinkerpop.blueprints.Direction.IN))
            return new Neo4jVertexEdgeIterable(this.graph, (Node) this.rawElement, Direction.INCOMING, labels);
        else
            return new MultiIterable(Arrays.asList(new Neo4jVertexEdgeIterable(this.graph, (Node) this.rawElement, Direction.OUTGOING, labels), new Neo4jVertexEdgeIterable(this.graph, (Node) this.rawElement, Direction.INCOMING, labels)));
    }

    public Iterable<Vertex> getVertices(com.tinkerpop.blueprints.Direction direction, String... labels) {
        this.graph.autoStartTransaction(false);
        if (direction.equals(com.tinkerpop.blueprints.Direction.OUT))
            return new Neo4jVertexVertexIterable(this.graph, (Node) this.rawElement, Direction.OUTGOING, labels);
        else if (direction.equals(com.tinkerpop.blueprints.Direction.IN))
            return new Neo4jVertexVertexIterable(this.graph, (Node) this.rawElement, Direction.INCOMING, labels);
        else
            return new MultiIterable(Arrays.asList(new Neo4jVertexVertexIterable(this.graph, (Node) this.rawElement, Direction.OUTGOING, labels), new Neo4jVertexVertexIterable(this.graph, (Node) this.rawElement, Direction.INCOMING, labels)));
    }

    public Edge addEdge(String label, Vertex vertex) {
        return this.graph.addEdge(null, this, vertex, label);
    }

    public Collection<String> getLabels() {
        this.graph.autoStartTransaction(false);
        Collection<String> labels = new ArrayList<String>();
        for (Label label : getRawVertex().getLabels()) {
            labels.add(label.name());
        }
        return labels;
    }

    public void addLabel(String label) {
        graph.autoStartTransaction(true);
        getRawVertex().addLabel(Label.label(label));
    }

    public void removeLabel(String label) {
        graph.autoStartTransaction(true);
        getRawVertex().removeLabel(Label.label(label));
    }

    public VertexQuery query() {
        this.graph.autoStartTransaction(false);
        return new DefaultVertexQuery(this);
    }

    public boolean equals(Object object) {
        return object instanceof Neo4j2Vertex && ((Neo4j2Vertex) object).getId().equals(this.getId());
    }

    public String toString() {
        return StringFactory.vertexString(this);
    }

    public Node getRawVertex() {
        return (Node) this.rawElement;
    }

    private class Neo4jVertexVertexIterable<T extends Vertex> implements Iterable<Neo4j2Vertex> {
        private final Neo4j2Graph graph;
        private final Node node;
        private final Direction direction;
        private final RelationshipType[] labels;

        public Neo4jVertexVertexIterable(Neo4j2Graph graph, Node node, Direction direction, String... labels) {
            this.graph = graph;
            this.node = node;
            this.direction = direction;
            this.labels = new RelationshipType[labels.length];
            for (int i = 0; i < labels.length; i++) {
                this.labels[i] = RelationshipType.withName(labels[i]);
            }
        }

        public Iterator<Neo4j2Vertex> iterator() {
            graph.autoStartTransaction(false);
            final Iterator<Relationship> itty;
            if (labels.length > 0)
                itty = node.getRelationships(direction, labels).iterator();
            else
                itty = node.getRelationships(direction).iterator();

            return new Iterator<Neo4j2Vertex>() {
                public Neo4j2Vertex next() {
                    graph.autoStartTransaction(false);
                    return new Neo4j2Vertex(itty.next().getOtherNode(node), graph);
                }

                public boolean hasNext() {
                    graph.autoStartTransaction(false);
                    return itty.hasNext();
                }

                public void remove() {
                    graph.autoStartTransaction(true);
                    itty.remove();
                }
            };
        }
    }

    private class Neo4jVertexEdgeIterable<T extends Edge> implements Iterable<Neo4j2Edge> {

        private final Neo4j2Graph graph;
        private final Node node;
        private final Direction direction;
        private final RelationshipType[] labels;

        public Neo4jVertexEdgeIterable(Neo4j2Graph graph, Node node, Direction direction, String... labels) {
            this.graph = graph;
            this.node = node;
            this.direction = direction;
            this.labels = new RelationshipType[labels.length];
            for (int i = 0; i < labels.length; i++) {
                this.labels[i] = RelationshipType.withName(labels[i]);
            }
        }

        public Iterator<Neo4j2Edge> iterator() {
            graph.autoStartTransaction(false);
            final Iterator<Relationship> itty;
            if (labels.length > 0)
                itty = node.getRelationships(direction, labels).iterator();
            else
                itty = node.getRelationships(direction).iterator();

            return new Iterator<Neo4j2Edge>() {
                public Neo4j2Edge next() {
                    graph.autoStartTransaction(false);
                    return new Neo4j2Edge(itty.next(), graph);
                }

                public boolean hasNext() {
                    graph.autoStartTransaction(false);
                    return itty.hasNext();
                }

                public void remove() {
                    graph.autoStartTransaction(true);
                    itty.remove();
                }
            };
        }
    }

}
