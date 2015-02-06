package eu.ehri.project.utils;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.persistence.ActionManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Map;

/**
 * Wraps Neo4jGraph and adds an extra method to allow asserting
 * that it should not be in a transaction.
 * <p/>
 * When starting a transaction we also lock the top node of
 * the graph-global action event chain in a assumption that graph modifications
 * will be audited and thus modify this node's relationships. The default
 * <a href="http://neo4j.com/docs/1.9/transactions-locking.html">Neo4j behaviour</a>
 * is just to lock nodes which are changed, which can occasionally lead concurrent
 * transactions to fail when the TXs attempt to modify the global action root relation
 * simultaneously. This solution should trade concurrent write speed for less chance
 * audited write actions will have TXs fail. Deadlocks should be detected and resolved
 * by Neo4j by failing a TX, but this has not yet been observed in practice.
 * <p/>
 * NB: On a cleanly initialised graph the action chain root node should be
 * node 0 but we cannot take this for granted. Instead we look the node
 * up by name in the index and cache it for subsequent invocations.
 */
public class TxCheckedNeo4jGraph extends Neo4jGraph {

    private static final String INDEX_NAME = "entities";

    private Node cachedEventChain = null;

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

    @Override
    public void autoStartTransaction() {
        Transaction transaction = tx.get();
        if (transaction == null) {
            transaction = getRawGraph().beginTx();
            tx.set(transaction);
            Node eventChain = getCachedEventChain();
            if (eventChain != null) {
                transaction.acquireWriteLock(eventChain);
            }
        }
    }

    /**
     * Throw an exception if we're currently in a transaction
     */
    public void checkNotInTransaction() {
        checkNotInTransaction("(no debug)");
    }

    /**
     * Throw an exception if we're currently in a transaction.
     *
     * @param msg a msg to use in the exception
     */
    public void checkNotInTransaction(String msg) {
        if (tx.get() != null) {
            rollback();
            throw new IllegalStateException("Error: graph is currently in a transaction: " + msg);
        }
    }

    /**
     * Checks if the graph is currently in a transaction.
     *
     * @return whether a transaction is held in this thread.
     */
    public boolean isInTransaction() {
        return tx.get() != null;
    }

    // Helper - look up the event chain in the index and cache it.
    private Node getCachedEventChain() {
        if (cachedEventChain == null) {
            IndexHits<Node> entities = getRawGraph().index()
                    .forNodes(INDEX_NAME).get(EntityType.ID_KEY, ActionManager
                            .GLOBAL_EVENT_ROOT);
            try {
                cachedEventChain = entities.getSingle();
            } finally {
                entities.close();
            }
        }
        return cachedEventChain;
    }
}
