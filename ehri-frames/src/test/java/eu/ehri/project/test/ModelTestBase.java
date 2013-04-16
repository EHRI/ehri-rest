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
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;
import eu.ehri.project.test.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.Crud;

public abstract class ModelTestBase extends GraphTestBase {

    protected Crud<DocumentaryUnit> views;
    protected FixtureLoader helper;

    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext())
            lst.add(it.next());
        return lst;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper = FixtureLoaderFactory.getInstance(graph);
        helper.loadTestData();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


}
