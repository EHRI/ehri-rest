package eu.ehri.project.views;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test promotion/demotion.
 *
 * NB: An item can be promoted multiple times (meaning there's
 * a "promotedBy" relationship from an item to a user. To
 * properly demote an item all "promotedBy" relationships must
 * be removed.
 *
 * NB2: This might be to change.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PromotionViewsTest extends AbstractFixtureTest {

    private UserProfile promoter;
    private UserProfile viewer;
    private AclManager acl;
    private PromotionViews promotionViews;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        promoter = manager.getFrame("tim", UserProfile.class);
        viewer = manager.getFrame("reto", UserProfile.class);
        acl = new AclManager(graph);
        promotionViews = new PromotionViews(graph);
    }

    @Test(expected = PromotionViews.NotPromotableError.class)
    public void testPromotingUnpromotableItemThrowsAnError() throws Exception {
        Annotation ann = manager.getFrame("ann3", Annotation.class);
        assertFalse(acl.canAccess(ann, viewer));
        promotionViews.promoteItem(ann, promoter);
        assertTrue(acl.canAccess(ann, viewer));
    }

    @Test
    public void testDemoteItem() throws Exception {
        Annotation ann = manager.getFrame("ann4", Annotation.class);
        assertTrue(acl.canAccess(ann, viewer));
        promotionViews.demoteItem(ann, promoter);
        assertFalse(acl.canAccess(ann, viewer));
    }

    @Test
    public void testDemoteItemByOtherUser() throws Exception {
        UserProfile promoter2 = manager.getFrame("mike", UserProfile.class);
        Annotation ann = manager.getFrame("ann4", Annotation.class);
        assertTrue(acl.canAccess(ann, viewer));
        promotionViews.demoteItem(ann, promoter2);
        // Item is *still* promoted by an other user
        // so we can still see it.
        assertTrue(acl.canAccess(ann, viewer));
    }

    @Test
    public void testPromoteItem() throws Exception {
        Annotation ann = manager.getFrame("ann5", Annotation.class);
        assertFalse(acl.canAccess(ann, viewer));
        promotionViews.promoteItem(ann, promoter);
        assertTrue(acl.canAccess(ann, viewer));
    }
}
