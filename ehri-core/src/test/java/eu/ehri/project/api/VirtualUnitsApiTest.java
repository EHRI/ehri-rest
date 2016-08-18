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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.api.VirtualUnitsApi;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class VirtualUnitsApiTest extends AbstractFixtureTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private VirtualUnitsApi views(Accessor accessor) {
        return api(accessor).virtualUnits();
    }

    @Test
    @Ignore
    public void testGetVirtualCollections() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        // Both of these units are present in vc1
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);

        Iterable<VirtualUnit> virtualCollectionsForC1 = views(validUser).getVirtualCollections(c1);
        Iterable<VirtualUnit> virtualCollectionsForC2 = views(validUser).getVirtualCollections(c4);
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC1));
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForC2));
    }

    @Test
    public void testGetVirtualCollectionsForUser() throws Exception {
        VirtualUnit vc1 = manager.getEntity("vc1", VirtualUnit.class);
        UserProfile linda = manager.getEntity("linda", UserProfile.class);
        Iterable<VirtualUnit> virtualCollectionsForUser = linda.getVirtualUnits();
        assertEquals(Lists.newArrayList(vc1), Lists.newArrayList(virtualCollectionsForUser));
    }

    @Test
    public void testMoveVirtualUnits() throws Exception {
        // move c1 from vu1 to vu2
        final DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        VirtualUnit vu1 = manager.getEntity("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getEntity("vu2", VirtualUnit.class);
        assertTrue(Iterables.contains(vu1.getIncludedUnits(), c1));
        assertFalse(Iterables.contains(vu2.getIncludedUnits(), c1));

        // Make a single-use iterator to test this works with streams.
        // The stream is iterated twice so it has to handle that correctly.
        Iterable<DocumentaryUnit> iter = () -> Lists.newArrayList(c1).iterator();
        views(validUser).moveIncludedUnits(vu1, vu2, iter);
        assertFalse(Iterables.contains(vu1.getIncludedUnits(), c1));
        assertTrue(Iterables.contains(vu2.getIncludedUnits(), c1));
    }
}
