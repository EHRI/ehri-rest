package eu.ehri.project.core;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface TxGraph extends Graph, TransactionalGraph, IndexableGraph {
    /**
     * Obtain a wrapped transaction object.
     *
     * @return a transaction wrapper
     */
    public Tx beginTx();

    /**
     * Determine if this graph is in a transaction.
     *
     * @return whether or not a transaction is open in this thread
     */
    public boolean isInTransaction();
}
