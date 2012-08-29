package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.views.Views;
import eu.ehri.project.test.DataLoader;

public class ViewsTest {
    protected FramedGraph<Neo4jGraph> graph;
    protected Views<DocumentaryUnit> views;
    protected DataLoader helper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        graph = new FramedGraph<Neo4jGraph>(
                new Neo4jGraph(
                        new TestGraphDatabaseFactory()
                            .newImpermanentDatabaseBuilder()
                            .newGraphDatabase()));
        helper = new DataLoader(graph);
        helper.loadTestData();
        
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    @Test
    public void testViews() {
        fail("Not yet implemented");
    }

    @Test
    public void testDetail() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdate() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreate() {
        fail("Not yet implemented");
    }

    @Test
    public void testDelete() {
        fail("Not yet implemented");
    }

}
