/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.core.impl;

import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps Neo4jGraph and adds an extra method to allow asserting
 * that it should not be in a transaction.
 */
public class TxNeo4jGraph extends Neo4j2Graph implements TxGraph {

    public static class UnderlyingTxRemovedError extends RuntimeException {
        UnderlyingTxRemovedError(String msg) {
            super(msg);
        }
    }

    public static final Logger logger = LoggerFactory.getLogger(TxNeo4jGraph.class);

    public TxNeo4jGraph(DatabaseManagementService service, GraphDatabaseService graph) {
        super(service, graph);
    }

    public TxNeo4jGraph(String directory) {
        super(directory);
    }

    private final ThreadLocal<Neo4jTx> etx = ThreadLocal.withInitial(() -> null);

    public Tx beginTx() {
        logger.trace("Begin tx: {}", Thread.currentThread().getName());
        if (this.tx.get() != null) {
            RuntimeException e = new RuntimeException("Tried to begin a TX when one is already open.");
            e.printStackTrace();
            throw e;
        }
        Transaction tx = getRawGraph().beginTx();
        this.tx.set(tx);
        Neo4jTx t = new Neo4jTx();
        etx.set(t);
        return t;
    }

    @Override
    public void commit() {
        if (tx.get() == null) {
            RuntimeException e = new RuntimeException("Attempting to commit null tx on: " + Thread.currentThread().getName());
            e.printStackTrace();
            throw e;
        }
        logger.trace("Committing TX on graph: {}", Thread.currentThread());
        super.commit();
        if (etx.get() != null) {
            logger.warn("Restarting Neo4j TX on {}", Thread.currentThread());
            // we are committing behind our backs. Switch out the TX of
            // EHRI TX with a new one...
            tx.set(getRawGraph().beginTx());
        }
    }

    @Override
    public void shutdown() {

        // FIXME: Neo4j 4
        getManagementService().shutdown();
    }

    /**
     * This overridden function is now a no-op.
     *
     * @param forWrite unused parameter
     */
    @Override
    public void autoStartTransaction(boolean forWrite) {
        // Not allowing auto-start TX
    }

    /**
     * Checks if the graph is currently in a transaction.
     *
     * @return whether a transaction is held in this thread.
     */
    public boolean isInTransaction() {
        return tx.get() != null;
    }

    public class Neo4jTx implements Tx {

        /**
         * Get the underlying transaction.
         *
         * @return a Neo4j transaction
         */
        public Transaction underlying() {
            return tx.get();
        }

        public void success() {
            logger.trace("Successful TX {} on: {}", this, Thread.currentThread());
            Transaction transaction = tx.get();
            if (transaction == null) {
                throw new UnderlyingTxRemovedError("Underlying transaction removed!");
            }
            transaction.commit();
        }

        public void close() {
            logger.trace("Closing TX {} on: {}", this, Thread.currentThread());
            Transaction transaction = tx.get();
            if (transaction == null) {
                throw new UnderlyingTxRemovedError("Underlying transaction removed!");
            }
            transaction.close();
            tx.remove();
            etx.remove();
        }

        public void failure() {
            logger.trace("Failed TX {} on: {}", this, Thread.currentThread());
            Transaction transaction = tx.get();
            if (transaction == null) {
                throw new UnderlyingTxRemovedError("Underlying transaction removed!");
            }
            transaction.rollback();
        }
    }
}
