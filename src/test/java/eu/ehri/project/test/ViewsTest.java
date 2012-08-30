package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.views.Views;
import eu.ehri.project.test.DataLoader;

public class ViewsTest {
    
    protected static final String TEST_COLLECTION_NAME = "A brand new collection";
    protected static final String TEST_START_DATE = "1945-01-01T00:00:00Z";
    
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
    public void testUpdate() throws PermissionDenied, ValidationError {
        Map<String,Object> bundle = getTestBundle();
        DocumentaryUnit unit = views.create(bundle, validUserId);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
        
        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_COLLECTION_NAME + " with new stuff";
        bundle.put("id", unit.asVertex().getId());
        
        Map<String,Object> data = (Map<String, Object>) bundle.get("data");        
        data.put("name", newName);
        
        DocumentaryUnit changedUnit = views.update(bundle, validUserId);
        assertEquals(newName, changedUnit.getName()); 
        
        // Check the nested item was created correctly
        DatePeriod datePeriod = changedUnit.getDatePeriods().iterator().next();
        assertTrue(datePeriod != null);
        assertEquals(TEST_START_DATE, datePeriod.getStartDate());
        
        // And that the reverse relationship works.
        assertEquals(changedUnit.asVertex(), datePeriod.getEntity().asVertex());
    }

    @Test
    public void testCreate() throws ValidationError, PermissionDenied {
        Map<String,Object> bundle = getTestBundle();
        Map<String,Object> data = (Map<String, Object>) bundle.get("data");
        String testName = (String) data.get("name");
        
        DocumentaryUnit unit = views.create(bundle, validUserId);
        assertEquals(testName, unit.getName());
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

    // Helpers
    
    @SuppressWarnings("serial")
    private Map<String,Object> getTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String,Object>() {{
            put("id", null);
            put("data", new HashMap<String,Object>() {{ 
                put("name", TEST_COLLECTION_NAME);
                put("identifier", "someid-01");                
                put("isA", EntityTypes.DOCUMENTARY_UNIT);                
            }});
            put("relationships", new HashMap<String,Object>() {{ 
                put("describes", new LinkedList<HashMap<String,Object>>() {{
                    add(new HashMap<String,Object>() {{
                        put("id", null);
                        put("data", new HashMap<String,Object>() {{
                            put("identifier", "someid-01");
                            put("title", "A brand new item description");
                            put("isA", EntityTypes.DOCUMENT_DESCRIPTION);
                            put("languageOfDescription", "en");
                        }});
                    }});                                            
                }});
                put("hasDate", new LinkedList<HashMap<String,Object>>() {{
                    add(new HashMap<String,Object>() {{
                        put("id", null);
                        put("data", new HashMap<String,Object>() {{ 
                            put("startDate", TEST_START_DATE);
                            put("endDate", TEST_START_DATE);
                            put("isA", EntityTypes.DATE_PERIOD);
                        }});
                    }});                                            
                }});
            }});
        }};
    }
}
