/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.views;

import com.google.common.collect.Iterables;
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
        promotionViews.upVote(ann, promoter);
        assertTrue(acl.canAccess(ann, viewer));
    }

    @Test
    public void testDemoteItem() throws Exception {
        Annotation ann = manager.getFrame("ann4", Annotation.class);
        assertTrue(acl.canAccess(ann, viewer));
        promotionViews.removeUpVote(ann, promoter);
        assertFalse(acl.canAccess(ann, viewer));
    }

    @Test
    public void testDemoteItemByOtherUser() throws Exception {
        UserProfile promoter2 = manager.getFrame("mike", UserProfile.class);
        Annotation ann = manager.getFrame("ann4", Annotation.class);
        assertTrue(acl.canAccess(ann, viewer));
        promotionViews.removeDownVote(ann, promoter2);
        // Item is *still* promoted by an other user
        // so we can still see it.
        assertTrue(acl.canAccess(ann, viewer));
    }

    @Test
    public void testPromoteItem() throws Exception {
        Annotation ann = manager.getFrame("ann5", Annotation.class);
        assertFalse(acl.canAccess(ann, viewer));
        promotionViews.upVote(ann, promoter);
        assertTrue(acl.canAccess(ann, viewer));
    }

    @Test
    public void testIsPromoted() throws Exception {
        Annotation ann = manager.getFrame("ann6", Annotation.class);
        assertFalse(ann.isPromoted());
        assertTrue(Iterables.contains(ann.getPromoters(), promoter));
        assertTrue(ann.isPromotedBy(promoter));
    }
}
