package eu.ehri.project.views;

import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class VirtualUnitViewsTest extends AbstractFixtureTest {
    private VirtualUnitViews views;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        views = new VirtualUnitViews(graph);
    }

    @Test
    @Ignore
    public void testGetVirtualCollections() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        // Both of these units are present in vc1
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);

        Iterable<VirtualUnit> virtualCollectionsForC1 = views.getVirtualCollections(c1, validUser);
        Iterable<VirtualUnit> virtualCollectionsForC2 = views.getVirtualCollections(c4, validUser);
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC1));
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC2));
    }

    @Test
    public void testGetVirtualCollectionsForUser() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        Accessor linda = manager.getFrame("linda", Accessor.class);
        Iterable<VirtualUnit> virtualCollectionsForUser
                = views.getVirtualCollectionsForUser(linda, validUser);
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForUser));
    }
}
