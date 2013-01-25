package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.impl.Query;
import eu.ehri.project.views.impl.Query.Page;

public class QueryTest extends AbstractFixtureTest {

    @Test
    public void testAdminCanListEverything() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        assertTrue(new AclManager(graph).belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // And the listing the ACL way...
        List<DocumentaryUnit> list = toList(query.list(validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());

        // Test the limit function
        list = toList(query.setLimit(1).list(validUser));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());

        // Test the offset function
        list = toList(query.setLimit(2).setOffset(1).list(validUser));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
    }

    @Test
    public void testPage() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        assertTrue(new AclManager(graph).belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Test the limit function
        Query.Page<DocumentaryUnit> page = query.setLimit(1).page(validUser);
        List<DocumentaryUnit> list = toList(page.getIterable());
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(toList(allDocs).size(), page.getCount());
    }

    @Test
    public void testUserCannotListPrivate() throws IndexNotFoundException,
            ItemNotFound {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        Accessor accessor = manager.getFrame("reto", Accessor.class);
        DocumentaryUnit cantRead = manager
                .getFrame("c1", DocumentaryUnit.class);
        assertFalse(new AclManager(graph).belongsToAdmin(accessor));

        List<DocumentaryUnit> list = toList(query.list(accessor));
        assertFalse(list.isEmpty());
        assertFalse(list.contains(cantRead));
    }

    @Test
    public void testListWithFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.setLimit(1).list(
                AccessibleEntity.IDENTIFIER_KEY, "c1", validUser));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testListWithDepthFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for only top-level documentary units.
        // The result should be c1 and c4
        List<DocumentaryUnit> list = toList(query.depthFilter(DocumentaryUnit.CHILD_OF, 0).list(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        // The same query with a depth filter of 1 should get 3 items
         list = toList(query.depthFilter(DocumentaryUnit.CHILD_OF, 1).list(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        
    }

    @Test
    public void testListWithPredicateFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.EQUALS,
                "c1").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());
        
        // Case-insensitive query
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.IEQUALS,
                "C1").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());

        // Startswith...
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.STARTSWITH,
                "c").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(4, list.size());
        
        // Endswith... should get one item (c1)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.ENDSWITH,
                "1").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());
        
        // Regexp... should get all doc units (c1-4)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.MATCHES,
                "^c\\d+$").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(4, list.size());
        
        // Less than... should get one item (c1)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.LT,
                "c2").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());
        assertEquals("c1", list.get(0).getIdentifier());
        
        // Greater than... should get one item (c4)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.GT,
                "c3").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());
        assertEquals("c4", list.get(0).getIdentifier());
        
        // Less than or equal... should get twos items (c1,c2)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.LTE,
                "c2").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(2, list.size());
        assertEquals("c1", list.get(0).getIdentifier());
        assertEquals("c2", list.get(1).getIdentifier());
        
        // Greater than or equal... should get two items (c3,c4)
        list = toList(query.filter(
                AccessibleEntity.IDENTIFIER_KEY, Query.FilterPredicate.GTE,
                "c3").list(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(2, list.size());
        assertEquals("c3", list.get(0).getIdentifier());
        assertEquals("c4", list.get(1).getIdentifier());
        
    }

    @Test
    public void testListWithSort() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        Page<DocumentaryUnit> page = query.orderBy(
                AccessibleEntity.IDENTIFIER_KEY, Query.Sort.ASC).page(
                EntityClass.DOCUMENTARY_UNIT, validUser);
        assertFalse(page.getCount() == 0);
        assertEquals("c1", toList(page.getIterable()).get(0).getIdentifier());

        page = query.orderBy(AccessibleEntity.IDENTIFIER_KEY, Query.Sort.DESC)
                .page(EntityClass.DOCUMENTARY_UNIT, validUser);
        assertFalse(page.getCount() == 0);

        // NB: This will break if other collections are added to the
        // fixtures. Adjust as necessary.
        assertEquals("c4", toList(page.getIterable()).get(0).getIdentifier());
    }

    @Test
    public void testListWithGlobFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Query for document identifier starting with 'c'.
        // In the fixtures this is ALL docs
        List<DocumentaryUnit> list = toList(query.list(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testListWithFailFilter() throws IndexNotFoundException {
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);

        // Do a query that won't match anything.
        List<DocumentaryUnit> list = toList(query.list(
                AccessibleEntity.IDENTIFIER_KEY, "__GONNAFAIL__", validUser));
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
        Accessor accessor = manager.getFrame("reto", Accessor.class);
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        query.get(AccessibleEntity.IDENTIFIER_KEY, "c1", accessor);
    }
}
