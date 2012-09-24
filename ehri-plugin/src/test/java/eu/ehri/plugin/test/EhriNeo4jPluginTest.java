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
import eu.ehri.project.test.utils.FixtureLoader;
import eu.ehri.project.views.Views;

import eu.ehri.plugin.EhriNeo4jPlugin;

public class EhriNeo4jPluginTest {
    protected FramedGraph<Neo4jGraph> graph;
    protected Views<DocumentaryUnit> views;
    protected FixtureLoader helper;
    protected EhriNeo4jPlugin plugin;
    protected GraphDatabaseService db;

    // Members closely coupled to the test data!
    protected Long validUserId = 20L;
    protected Long invalidUserId = 21L;
    protected Long itemId = 1L;
    protected String itemName = "Test Collection 1";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        helper = new FixtureLoader(graph);
        helper.loadTestData();
        views = new Views<DocumentaryUnit>(graph, DocumentaryUnit.class);

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
        Representation unit = plugin.getVertexIndex(db, "nonexistingindex");
    }

    // lets see If I can do the same details test as already done in the
    // ehri-frames test
    @Test
    public void testDetail() throws PermissionDenied {
        DocumentaryUnit unit = views.detail(itemId, validUserId);
        assertEquals(itemId, unit.asVertex().getId());
    }

    // Then do the same but via the plugin
    @Test
    public void testGetDocumentartyUnit() throws Exception {
        Representation unit = plugin
                .getDocumentaryUnit(db, itemId, validUserId);
    }
}
