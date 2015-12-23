package eu.ehri.project.core;

import com.tinkerpop.blueprints.TransactionalGraph;


public interface TxGraph extends TransactionalGraph {
    /**
     * Obtain a wrapped transaction object.
     *
     * @return a transaction wrapper
     */
    Tx beginTx();

    /**
     * Determine if this graph is in a transaction.
     *
     * @return whether or not a transaction is open in this thread
     */
    boolean isInTransaction();
}
