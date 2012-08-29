package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.views.Views;
import eu.ehri.project.test.DataLoader;

public class ViewsTest {
    protected FramedGraph<Neo4jGraph> graph;
    protected Views<DocumentaryUnit> views;
    protected DataLoader helper;
    
    // Members closely coupled to the test data!
    protected Long validUserId = 20L;
    protected Long invalidUserId = 21L;
    protected Long itemId = 1L;

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
        views = new Views<DocumentaryUnit>(
                graph, DocumentaryUnit.class);   
    }

    @After
    public void tearDown() throws Exception {
        //graph.shutdown();
    }

    /**
     * Access an item 0 as user 20.
     * @throws PermissionDenied 
     */
    @Test
    public void testDetail() throws PermissionDenied {
        DocumentaryUnit unit = views.detail(itemId, validUserId);
        assertEquals(itemId, unit.asVertex().getId());
    }

    /**
     * Access an item as an invalid user. TODO: Check
     * that this test fails if no exception is thrown.
     * 
     * @throws PermissionDenied
     */
    @Test(expected=PermissionDenied.class)
    public void testDetailPermissionDenied() throws PermissionDenied {
        views.detail(itemId, invalidUserId);
    }

    @Test
    public void testUpdate() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreate() {
        fail("Not yet implemented");
    }

    /**
     * Tests that deleting a collection will also delete
     * its dependent relations. NB: This test will break
     * of other @Dependent relations are added to
     * DocumentaryUnit.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     */
    @Test
    public void testDelete() throws PermissionDenied, ValidationError {
        Integer shouldDelete = 1;
        DocumentaryUnit unit = graph.getVertex(itemId, DocumentaryUnit.class);
        
        // FIXME: Surely there's a better way of doing this???
        Iterator<DatePeriod> dateIter = unit.getDatePeriods().iterator();
        Iterator<Description> descIter = unit.getDescriptions().iterator();
        for (; dateIter.hasNext(); shouldDelete++) dateIter.next();
        for (; descIter.hasNext(); shouldDelete++) descIter.next();

        Integer deleted = views.delete(itemId, validUserId);
        assertEquals(shouldDelete, deleted);
    }

}
