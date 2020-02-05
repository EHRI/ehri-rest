package eu.ehri.project.core.impl.neo4j;


import com.tinkerpop.blueprints.CloseableIterable;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
//import org.neo4j.graphdb.index.IndexHits;

import java.util.Iterator;


public class Neo4j2VertexIterable<T extends Neo4j2Vertex> implements CloseableIterable<Neo4j2Vertex> {

    private final ResourceIterable<Node> nodes;
    private final Neo4j2Graph graph;

    public Neo4j2VertexIterable(ResourceIterable<Node> nodes, Neo4j2Graph graph) {
        this.nodes = nodes;
        this.graph = graph;
    }

    public Iterator<Neo4j2Vertex> iterator() {
        graph.autoStartTransaction(false);
        return new Iterator<Neo4j2Vertex>() {
            private final Iterator<Node> itty = nodes.iterator();

            public void remove() {
                this.itty.remove();
            }

            public Neo4j2Vertex next() {
                graph.autoStartTransaction(false);
                return new Neo4j2Vertex(this.itty.next(), graph);
            }

            public boolean hasNext() {
                graph.autoStartTransaction(false);
                return this.itty.hasNext();
            }
        };
    }

    public void close() {
        // FIXME: Neo4j 4
//        if (this.nodes instanceof IndexHits) {
//            ((IndexHits<?>) this.nodes).close();
//        }
    }

}