package eu.ehri.project.test.utils;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;

/**
 * Deletes all nodes and indices from a Neo4j graph. Use with care.
 * 
 * Note: This does NOT reset the Neo4j node auto-increment id.
 * 
 * @author michaelb
 * 
 */
public class GraphCleaner<T extends TransactionalGraph & IndexableGraph> {
        
    private FramedGraph<T> graph;
    
    /**
     * Constructor.
     * 
     * @param graph
     */
    public GraphCleaner(FramedGraph<T> graph) {
        this.graph = graph;
    }
    
    /**
     * Delete all nodes and indices from the graph.
     */
    public void clean() {
        try {
            for (Index<? extends Element> idx : graph.getBaseGraph().getIndices()) {
                graph.getBaseGraph().dropIndex(idx.getIndexName());
            }
            /*for (Vertex v : graph.getVertices()) {
                // FIXME: Neo4j deadlocks when attempting to delete all nodes,
                // whether it's done directly, or with Cypher, or whatever.
                // So just hope that deindexing then will suffice here.
                //graph.removeVertex(v);
                for (String key : v.getPropertyKeys()) {
                    System.out.println(" - removing prop : " + key + " (" + v.getId() + ")");
                    v.removeProperty(key);
                }
            }*/
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }
    }
}
