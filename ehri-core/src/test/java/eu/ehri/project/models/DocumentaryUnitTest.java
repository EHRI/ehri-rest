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
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.*;


public class DocumentaryUnitTest extends AbstractFixtureTest {

    @Test
    public void testGetAncestorsAndSelf() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getEntity("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getEntity("c3", DocumentaryUnit.class);
        Iterable<DocumentaryUnit> ancestorsAndSelf = c3.getAncestorsAndSelf();
        assertEquals(Lists.newArrayList(c3, c2, c1), Lists.newArrayList(ancestorsAndSelf));
    }

    @Test
    public void testGetVirtualCollections() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        // All of these units belong to vc1 except nl-r1-m19
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getEntity("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getEntity("c3", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        DocumentaryUnit c5 = manager.getEntity("nl-r1-m19", DocumentaryUnit.class);

        Iterable<VirtualUnit> virtualCollectionsForC1 = c1.getVirtualCollections();
        Iterable<VirtualUnit> virtualCollectionsForC2 = c2.getVirtualCollections();
        Iterable<VirtualUnit> virtualCollectionsForC3 = c3.getVirtualCollections();
        Iterable<VirtualUnit> virtualCollectionsForC4 = c4.getVirtualCollections();
        Iterable<VirtualUnit> virtualCollectionsForC5 = c5.getVirtualCollections();
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC1));
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC2));
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC3));
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC4));
        assertEquals(Lists.newArrayList(), Lists.newArrayList(virtualCollectionsForC5));
    }


    @Test
    public void testCollectionHelpByRepo() throws ItemNotFound {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        assertNotNull(unit.getRepository());
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testChildDocsCanAccessTheirAgent() throws ItemNotFound {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getEntity("c3", DocumentaryUnit.class);
        assertNotNull(child.getRepository());
        assertNotNull(unit.getRepository());
        assertNotNull(unit.getRepositoryIfTopLevel());
        assertNull(child.getRepositoryIfTopLevel());
        assertEquals(unit.getRepository(), child.getRepository());
    }

    @Test
    public void testCannotAddChildOfRelationshipTwice() throws Exception {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getEntity("c2", DocumentaryUnit.class);
        assertEquals(unit, child.getParent());
        assertEquals(1, unit.getChildCount());
        unit.addChild(child);
        assertEquals(1, unit.getChildCount());
    }

    @Test
    public void testCannotAddSelfAsChild() throws Exception {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        assertEquals(1, unit.getChildCount());
        unit.addChild(unit);
        assertEquals(1, unit.getChildCount());
    }

    @Test
    public void testParentChildRelationship() throws ItemNotFound {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getEntity("c2", DocumentaryUnit.class);
        DocumentaryUnit child2 = manager.getEntity("c3", DocumentaryUnit.class);
        assertEquals(unit, child.getParent());
        assertEquals(child, child2.getParent());
        assertTrue(Iterables.contains(unit.getChildren(), child));
        assertTrue(Iterables.contains(unit.getAllChildren(), child));
        assertTrue(Iterables.contains(unit.getAllChildren(), child2));
        assertTrue(Iterables.contains(unit.getAllChildren(), child2));
    }
}
