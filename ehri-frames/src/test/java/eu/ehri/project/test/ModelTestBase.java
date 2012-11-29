package eu.ehri.project.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;
import eu.ehri.project.test.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.Views;

public class ModelTestBase {

    protected FramedGraph<Neo4jGraph> graph;
    protected Views<DocumentaryUnit> views;
    protected GraphManager manager;
    protected FixtureLoader helper;

    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext())
            lst.add(it.next());
        return lst;
    }

    @Before
    public void setUp() {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        manager = new GraphManager(graph);
        helper = new FixtureLoaderFactory().getInstance(graph);
        helper.loadTestData();
    }
    
    @After
    public void tearDown() {
        graph.shutdown();
    }
}
