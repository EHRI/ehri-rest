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

package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



public class VirtualUnitTest extends AbstractFixtureTest {
    @Test
    public void testGetChildCount() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        assertEquals(1, vc1.countChildren());
    }

    @Test
    public void testGetParent() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        assertEquals(vc1, vu1.getParent());
    }

    @Test
    public void testAddChild() throws Exception {
        VirtualUnit vu2 = manager.getEntity("vu2", VirtualUnit.class);
        VirtualUnit vu3 = manager.getEntity("vu3", VirtualUnit.class);
        int childCount = vu2.countChildren();
        assertTrue(vu2.addChild(vu3));
        assertEquals(childCount + 1, vu2.countChildren());
        // Doing the same thing twice should return false
        assertFalse(vu2.addChild(vu3));
    }

    @Test
    public void testAddChildWithBadChild() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        VirtualUnit vc1Alt = manager.getEntity("vc1", VirtualUnit.class);
        // This shouldn't be allowed!
        assertFalse(vc1.addChild(vc1));
        // Nor should this - loop
        assertFalse(vc1Alt.addChild(vc1));
    }

    @Test
    public void testGetIncludedUnits() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        assertTrue(vu1.getIncludedUnits().iterator().hasNext());
        assertEquals(c1, vu1.getIncludedUnits().iterator().next());
    }

    @Test
    public void testChildCount() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        VirtualUnit vu3 = manager.getEntity("vu3", VirtualUnit.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        assertTrue(vu1.getAllChildren().iterator().hasNext());
        assertTrue(vu1.getIncludedUnits().iterator().hasNext());
        int childCount = vu1.countChildren();
        assertEquals(2, childCount);
        vu1.addIncludedUnit(c4);
        assertEquals(childCount + 1, vu1.countChildren());
        vu1.removeIncludedUnit(c4);
        assertEquals(childCount, vu1.countChildren());
        vu1.addChild(vu3);
        assertEquals(childCount + 1, vu1.countChildren());
        vu1.removeChild(vu3);
        assertEquals(childCount, vu1.countChildren());

    }

    @Test
    public void testAddIncludedUnit() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        assertEquals(1, Iterables.size(vu1.getIncludedUnits()));
        vu1.addIncludedUnit(c4);
//        vu1.addReferencedDescription(cd4);
        assertEquals(2, Iterables.size(vu1.getIncludedUnits()));
        // check we can't add it twice
        vu1.addIncludedUnit(c4);
        assertEquals(2, Iterables.size(vu1.getIncludedUnits()));
    }

    @Test
    public void testRemoveIncludedUnit() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        vu1.removeIncludedUnit(c1);
        assertFalse(Lists.newArrayList(vu1.getIncludedUnits()).contains(c1));
    }

    @Test
    public void testGetVirtualDescriptions() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        DocumentaryUnitDescription cd1 = manager.getEntity("vcd1", DocumentaryUnitDescription.class);
        assertTrue(vc1.getVirtualDescriptions().iterator().hasNext());
        assertEquals(cd1, vc1.getVirtualDescriptions().iterator().next());
    }

    @Test
    public void testGetAncestors() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getEntity("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu1, vc1), Lists.newArrayList(vu2.getAncestors()));
    }

    @Test
    public void testGetChildren() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getEntity("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu2), Lists.newArrayList(vu1.getChildren()));
    }

    @Test
    public void testGetAllChildren() throws Exception {
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getEntity("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu2), Lists.newArrayList(vu1.getAllChildren()));
    }

    @Test
    public void testGetAuthor() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        UserProfile linda = manager.getEntity("linda", UserProfile.class);
        assertEquals(linda, vc1.getAuthor());
    }

    @Test
    public void testSetAuthor() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        UserProfile linda = manager.getEntity("linda", UserProfile.class);
        Group kcl = manager.getEntity("kcl", Group.class);
        assertEquals(linda, vc1.getAuthor());
        vc1.setAuthor(kcl);
        assertEquals(kcl, vc1.getAuthor());
    }
}
