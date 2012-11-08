package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.Query;

public class QueryTest extends AbstractFixtureTest {

    @Test
    public void testAdminCanListEverything() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        assertTrue(new AclManager(graph).belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = graph.getVertices(EntityType.KEY,
                EntityTypes.DOCUMENTARY_UNIT);

        // And the listing the ACL way...
        List<DocumentaryUnit> list = toList(query.list(null, null, validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());

        // Test the limit function
        list = toList(query.list(0, 1, validUser));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());

        // Test the offset function
        list = toList(query.list(1, 2, validUser));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
    }

    @Test
    public void testUserCannotListPrivate() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        Accessor accessor = helper.getTestFrame("reto", Accessor.class);
        DocumentaryUnit cantRead = helper.getTestFrame("c1",
                DocumentaryUnit.class);
        assertFalse(new AclManager(graph).belongsToAdmin(accessor));

        List<DocumentaryUnit> list = toList(query.list(null, null, accessor));
        assertFalse(list.isEmpty());
        assertFalse(list.contains(cantRead));
    }

    @Test
    public void testListWithFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "c1", 0, 1, validUser));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testListWithGlobFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = graph.getVertices(EntityType.KEY,
                EntityTypes.DOCUMENTARY_UNIT);

        // Query for document identifier starting with 'c'.
        // In the fixtures this is ALL docs
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "c*", null, null, validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testListWithFailFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Do a query that won't match anything.
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "__GONNAFAIL__", null, null, validUser));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testGet() throws PermissionDenied, ItemNotFound,
            IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        DocumentaryUnit doc = query.get(AccessibleEntity.IDENTIFIER_KEY, "c1",
                validUser);
        assertEquals("c1", doc.getIdentifier());
    }

    @Test(expected = ItemNotFound.class)
    public void testGetItemNotFound() throws PermissionDenied, ItemNotFound,
            IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        query.get(AccessibleEntity.IDENTIFIER_KEY, "IDONTEXIST", validUser);
    }

    @Test(expected = PermissionDenied.class)
    public void testGetPermissionDenied() throws PermissionDenied,
            ItemNotFound, IndexNotFoundException {
        Accessor accessor = helper.getTestFrame("reto", Accessor.class);
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        query.get(AccessibleEntity.IDENTIFIER_KEY, "c1", accessor);
    }
}
