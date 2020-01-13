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

package eu.ehri.project.core;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Uses the embedded neo4j database initial code from the neo4j tutorials
 * 
 */
public class Neo4jBasicTest {
    protected GraphDatabaseService graphDb;

    @Before
    public void prepareTestDatabase() {
        graphDb = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder().newGraphDatabase();
    }

    @After
    public void destroyTestDatabase() {
        graphDb.shutdown();
    }

    @Test(expected = org.neo4j.graphdb.NotInTransactionException.class)
    public void notAllowCountingNodesOutsideOfATx() {
        graphDb.getAllNodes();
    }

    @Test
    public void shouldAllowCountingNodes() {
        try (Transaction tx = graphDb.beginTx()) {
            Iterable<Node> nodes = graphDb.getAllNodes();
            assertEquals(0, Iterables.size(nodes));
            tx.success();
        }
    }

    @Test
    public void shouldCreateNode() {
        try (Transaction tx = graphDb.beginTx()) {
            Node n = graphDb.createNode();
            n.setProperty("name", "Nancy");

            // The node should have an id of 0, being the first node
            // in the graph.
            assertNotNull(n.getId());
            assertEquals(0, n.getId());

            // Retrieve a node by using the id of the created node. The id's and
            // property should match.
            Node foundNode = graphDb.getNodeById(n.getId());
            assertEquals(foundNode.getId(), n.getId());
            assertEquals(foundNode.getProperty("name"), "Nancy");

            assertEquals(1, Iterables.size(graphDb.getAllNodes()));
            tx.success();
        }
    }
}
