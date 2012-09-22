package eu.ehri.project.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.utils.FixtureLoader;
import eu.ehri.project.views.Views;

public class ModelTestBase {

    protected FramedGraph<Neo4jGraph> graph;
    protected Views<DocumentaryUnit> views;
    protected FixtureLoader helper;

    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext())
            lst.add(it.next());
        return lst;
    }

    public void setUp() {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        helper = new FixtureLoader(graph);
        helper.loadTestData();
    }

    public void tearDown() throws Exception {
        graph.shutdown();
    }
}
