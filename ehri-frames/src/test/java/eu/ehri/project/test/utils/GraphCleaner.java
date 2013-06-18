package eu.ehri.project.test.utils;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.List;

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
