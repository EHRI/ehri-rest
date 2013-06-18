package eu.ehri.extension.utils;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Map;

/**
 * Created by mike on 18/06/13.
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

    public void clearTxThreadVar() {
        if (tx.get() != null) {
            System.err.println("Transaction threadvar is not empty!!!!! Bug somewhere in clearing the transaction!!!");
            rollback();
            throw new IllegalStateException("Transaction thread var is not empty!!!");
        }
    }
}
