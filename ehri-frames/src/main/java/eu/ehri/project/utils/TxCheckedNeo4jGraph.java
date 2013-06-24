package eu.ehri.project.utils;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Map;

/**
 * Created by mike on 18/06/13.
 *
 * Wraps Neo4jGraph and adds an extra method to allow asseting
 * that it should not be in a transaction.
 */
public class TxCheckedNeo4jGraph extends Neo4jGraph {
    public TxCheckedNeo4jGraph(String directory) {
        super(directory);
    }

    public TxCheckedNeo4jGraph(GraphDatabaseService rawGraph) {
        super(rawGraph);
    }

    public TxCheckedNeo4jGraph(GraphDatabaseService rawGraph, boolean fresh) {
        super(rawGraph, fresh);
    }

    public TxCheckedNeo4jGraph(String directory, Map<String, String> configuration) {
        super(directory, configuration);
    }

    public void checkNotInTransaction() {
        if (tx.get() != null) {
            rollback();
            throw new IllegalStateException("Error: graph is currently in a transaction.");
        }
    }

    public boolean isInTransaction() {
        return tx.get() != null;
    }
}
