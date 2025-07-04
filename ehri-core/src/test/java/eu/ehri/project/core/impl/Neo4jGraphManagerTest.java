package eu.ehri.project.core.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Vertex;
import eu.ehri.project.models.EntityClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

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

    private Neo4j2Vertex createTestVertex(String id, EntityClass type) throws Exception {
        return (Neo4j2Vertex)manager.createVertex(id, type,
                Maps.newHashMap());
    }
}