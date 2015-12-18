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

package eu.ehri.project.models.base;

import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PromotableTest extends AbstractFixtureTest {

    private UserProfile user1;
    private UserProfile user2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        user1 = manager.getFrame("tim", UserProfile.class);
        user2 = manager.getFrame("linda", UserProfile.class);
    }

    @Test
    public void testGetPromoters() throws Exception {
        Promotable promotable = manager.getFrame("ann6", Promotable.class);
        assertTrue(promotable.getPromoters().iterator().hasNext());
        assertEquals(user1, promotable.getPromoters().iterator().next());
    }

    @Test
    public void testGetDemoters() throws Exception {
        Promotable promotable = manager.getFrame("ann6", Promotable.class);
        assertTrue(promotable.getDemoters().iterator().hasNext());
        assertEquals(user2, promotable.getDemoters().iterator().next());
    }

    @Test
    public void testAddPromotion() throws Exception {
        Promotable promotable = manager.getFrame("ann5", Promotable.class);
        assertEquals(0L, promotable.getPromotionScore());
        promotable.addPromotion(user1);
        assertEquals(1L, promotable.getPromotionScore());
    }

    @Test
    public void testAddDemotion() throws Exception {
        Promotable promotable = manager.getFrame("ann5", Promotable.class);
        assertEquals(0L, promotable.getPromotionScore());
        promotable.addDemotion(user1);
        assertEquals(-1L, promotable.getPromotionScore());
    }

    @Test
    public void testRemovePromotion() throws Exception {
        Promotable promotable = manager.getFrame("ann6", Promotable.class);
        assertEquals(0L, promotable.getPromotionScore());
        promotable.removePromotion(user1);
        assertEquals(-1L, promotable.getPromotionScore());
    }

    @Test
    public void testRemoveDemotion() throws Exception {
        Promotable promotable = manager.getFrame("ann6", Promotable.class);
        assertEquals(0L, promotable.getPromotionScore());
        promotable.removeDemotion(user2);
        assertEquals(1L, promotable.getPromotionScore());
    }

    @Test
    public void testIsPromoted() throws Exception {
        Promotable promotable = manager.getFrame("ann4", Promotable.class);
        assertTrue(promotable.isPromoted());
    }

    @Test
    public void testIsPromotedBy() throws Exception {
        Promotable promotable = manager.getFrame("ann4", Promotable.class);
        assertTrue(promotable.isPromotedBy(user1));
    }

    @Test
    public void testIsPromotable() throws Exception {
        Promotable promotable = manager.getFrame("ann5", Promotable.class);
        assertTrue(promotable.isPromotable());
    }

    @Test
    public void testUpdatePromotionScoreCache() throws Exception {
        Promotable promotable = manager.getFrame("ann5", Promotable.class);
        assertTrue(promotable.isPromotable());
    }
}
