/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.core.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.models.EntityClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for Neo4jGraphManager-specific functionality.
 */
public class Neo4jGraphManagerTest {

    private GraphManager manager;
    private FramedGraph<Neo4j2Graph> graph;

    @Before
    public void setUp() throws Exception {
        graph = new FramedGraphFactory().create(new Neo4j2Graph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        manager = new Neo4jGraphManager<>(graph);
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    @Test
    public void testCreateVertex() throws Exception {
        Neo4j2Vertex vertex = createTestVertex("foo", EntityClass.DOCUMENTARY_UNIT);
        List<String> labels = Lists.newArrayList(vertex.getLabels());
        assertEquals(2, labels.size());
        assertThat(labels, hasItem(Neo4jGraphManager.BASE_LABEL));
        assertThat(labels, hasItem(EntityClass.DOCUMENTARY_UNIT.toString()));
    }

    @Test
    public void testUpdateVertex() throws Exception {
        String testId = "foo";
        createTestVertex(testId, EntityClass.DOCUMENTARY_UNIT);
        Neo4j2Vertex updated = (Neo4j2Vertex)manager.updateVertex(testId, EntityClass.REPOSITORY,
                Maps.newHashMap());
        List<String> updatedLabels = Lists.newArrayList(updated.getLabels());
        assertEquals(2, updatedLabels.size());
        assertThat(updatedLabels, hasItem(Neo4jGraphManager.BASE_LABEL));
        assertThat(updatedLabels, hasItem(EntityClass.REPOSITORY.toString()));
    }

    @Test
    public void testCreationProperties() throws Exception {
        Vertex vertex = createTestVertex("n1", EntityClass.REPOSITORY);
        vertex.setProperty("__pid", "1234");

        assertEquals("1234", vertex.getProperty("__pid"));
    }

    @Test
    public void testPidUniqueAcrossEntityTypes() throws Exception {
        // Schema constraints (including the per-type @Unique constraint on
        // PID_KEY) are only created when the manager is initialized - this
        // does not happen automatically in this test's setUp().
        try (Transaction schemaTx = graph.getBaseGraph().getRawGraph().beginTx()) {
            Neo4jGraphManager.createIndicesAndConstraints(graph.getBaseGraph().getRawGraph());
            schemaTx.success();
        }

        Map<String, Object> data1 = Maps.newHashMap();
        data1.put(Ontology.PID_KEY, "same-pid");
        manager.createVertex("unit1", EntityClass.DOCUMENTARY_UNIT, data1);

        Map<String, Object> data2 = Maps.newHashMap();
        data2.put(Ontology.PID_KEY, "same-pid");
        try {
            manager.createVertex("repo1", EntityClass.REPOSITORY, data2);
            fail("Creating an entity of a different type with a duplicate PID should not be allowed");
        } catch (IntegrityError e) {
            // Expected: PID uniqueness must be enforced across all entity
            // types, not just within a single type's label.
        }
    }

    private Neo4j2Vertex createTestVertex(String id, EntityClass type) throws Exception {
        return (Neo4j2Vertex)manager.createVertex(id, type,
                Maps.newHashMap());
    }
}