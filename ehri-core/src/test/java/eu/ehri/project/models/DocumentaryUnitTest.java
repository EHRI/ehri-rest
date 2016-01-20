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
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class DocumentaryUnitTest extends AbstractFixtureTest {

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
        assertEquals(1L, unit.getChildCount());
        unit.addChild(child);
        assertEquals(1L, unit.getChildCount());
    }

    @Test
    public void testCannotAddSelfAsChild() throws Exception {
        DocumentaryUnit unit = manager.getEntity("c1", DocumentaryUnit.class);
        assertEquals(1L, unit.getChildCount());
        unit.addChild(unit);
        assertEquals(1L, unit.getChildCount());
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
