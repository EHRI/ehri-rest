package eu.ehri.project.test.utils;

import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexManager;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

/**
 * Deletes all nodes and indices from a Neo4j graph. Use with care.
 * 
 * Note: This does NOT reset the Neo4j node auto-increment id.
 * 
 * @author michaelb
 * 
 */
public class GraphCleaner {
        
    private FramedGraph<Neo4jGraph> graph;
    
    /**
     * Constructor.
     * 
     * @param graph
     */
    public GraphCleaner(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
    }
    
    /**
     * Delete all nodes and indices from the graph.
     */
    public void clean() {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            IndexManager manager = graph.getBaseGraph().getRawGraph().index();
            for (String nodeIndex : manager.nodeIndexNames()) {
                graph.getBaseGraph().dropIndex(nodeIndex);
            }
            for (String relIndex : manager.relationshipIndexNames()) {
                graph.getBaseGraph().dropIndex(relIndex);
            }
            for (Vertex v : graph.getVertices()) {
                if (!v.getId().equals(0L))
                    graph.removeVertex(v);
            }
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }
}
