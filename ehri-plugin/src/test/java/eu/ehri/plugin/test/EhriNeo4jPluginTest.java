package eu.ehri.plugin.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;
import eu.ehri.project.test.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.impl.CrudViews;

import eu.ehri.plugin.EhriNeo4jPlugin;

public class EhriNeo4jPluginTest {
    protected FramedGraph<Neo4jGraph> graph;
    protected FixtureLoader helper;
    protected EhriNeo4jPlugin plugin;
    protected GraphDatabaseService db;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        helper = new FixtureLoaderFactory().getInstance(graph);
        helper.loadTestData();
        plugin = new EhriNeo4jPlugin();
        db = graph.getBaseGraph().getRawGraph();
    }

    @After
    public void tearDown() throws Exception {
        // graph.shutdown();
    }

    // lets simple things using the fixture
    @Test(expected = IndexNotFoundException.class)
    public void createIndexedVertex() throws Exception {
        plugin.getVertexIndex(db, "nonexistingindex");
    }
}
