/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import com.google.common.collect.Iterables;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.*;

/**
 * This tests EHRI-specific functionalities for the subclass of
 * Neo4j2Graph we use for the command line utilities and the web
 * service. Essentially this overrides all the transaction-handling
 * behaviour and substitutes a more Neo4j-specific system.
 */
public class TxNeo4JGraphTest {

    private TxNeo4jGraph graph;

    @Before
    public void setUp() throws Exception {
        GraphDatabaseService rawGraph = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .newGraphDatabase();
        graph = new TxNeo4jGraph(rawGraph);
    }

    @After
    public void tearDown() throws Exception {
        try {
            graph.shutdown();
        } catch (Exception e) {
            // Ignoring problems here...
        }
    }

    @Test(expected = NotInTransactionException.class)
    public void testNodeCountNotInTx() {
        Iterables.size(graph.getVertices());
    }

    @Test
    public void testNodeCount() {
        try (@SuppressWarnings("unused") Tx tx = graph.beginTx()) {
            assertEquals(0, Iterables.size(graph.getVertices()));
        }
    }

    @Test
    public void testIsInTransaction() {
        try (@SuppressWarnings("unused") Tx tx = graph.beginTx()) {
            assertTrue(graph.isInTransaction());
        }
        assertFalse(graph.isInTransaction());
    }

    @Test
    public void testReplaceWrappedTx() {
        try (Tx tx = graph.beginTx()) {
            Transaction ntx1 = ((TxNeo4jGraph.Neo4jTx)tx).underlying();
            graph.commit();
            Transaction ntx2 = ((TxNeo4jGraph.Neo4jTx)tx).underlying();
            assertNotEquals(ntx1, ntx2);
        }
    }
}