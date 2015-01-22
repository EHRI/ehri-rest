package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenericViewsTest extends AbstractFixtureTest {

    private GenericViews views;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        views = new GenericViews(graph);
    }

    @Test
    public void testGet() throws Exception {
        assertEquals(validUser, views.get(validUser.getId(), validUser));
    }

    @Test
    public void testGetByGid() throws Exception {
        assertEquals(validUser, views.getByGid(validUser.asVertex().getId(), validUser));
    }

    @Test(expected = AccessDenied.class)
    public void testGetWithAccessDenied() throws Exception {
        views.get("c1", invalidUser);
    }

    @Test(expected = ItemNotFound.class)
    public void testGetWithNotFound() throws Exception {
        views.get("NOT-AN-ITEM", invalidUser);
    }

    @Test(expected = ItemNotFound.class)
    public void testGetWithNoContentType() throws Exception {
        // Item exists but is not a content type so cannot be
        // generically plucked from the DB
        views.get("c1-dp1", validUser);
    }

    @Test
    public void testListByGid() throws Exception {
        Iterable<Vertex> byGid = views
                .listByGid(Lists.newArrayList(validUser.asVertex().getId()), validUser, false);
        assertEquals(1, Iterables.size(byGid));
        Iterable<Vertex> byGidEmpty = views
                .listByGid(Lists.<Object>newArrayList(-1L), validUser, false);
        assertEquals(0, Iterables.size(byGidEmpty));
    }

    @Test(expected = ItemNotFound.class)
    public void testListByGidStrict() throws Exception {
        views.listByGid(Lists.<Object>newArrayList(-1L), validUser, true);
    }

    @Test
    public void testList() throws Exception {
        Iterable<Vertex> byId = views
                .list(Lists.newArrayList(validUser.getId()), validUser, false);
        assertEquals(1, Iterables.size(byId));
        Iterable<Vertex> byIdEmpty = views
                .list(Lists.newArrayList("NOT-AN-ITEM"), validUser, false);
        assertEquals(0, Iterables.size(byIdEmpty));

    }

    @Test(expected = ItemNotFound.class)
    public void testListStrict() throws Exception {
        views.list(Lists.newArrayList("NOT-AN-ITEM"), validUser, true);
    }

    @Test
    public void testListWithAccessFilter() throws Exception {
        Iterable<Vertex> byId = views
                .list(Lists.newArrayList("c1"), invalidUser, true);
        assertEquals(0, Iterables.size(byId));
    }

    @Test
    public void testListWithNoContentType() throws Exception {
        Iterable<Vertex> byId = views
                .list(Lists.newArrayList("c1-dp1"), validUser, true);
        assertEquals(0, Iterables.size(byId));
    }
}