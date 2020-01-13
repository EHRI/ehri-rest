/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class LinkTest extends AbstractFixtureTest {
    @Test
    public void testGetLinker() throws Exception {
        Link link = manager.getEntity("link1", Link.class);
        assertEquals(validUser, link.getLinker());
    }

    @Test
    public void testGetLinkTargets() throws Exception {
        Link link = manager.getEntity("link1", Link.class);
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        assertTrue(Iterables.contains(link.getLinkTargets(), c1));
        assertTrue(Iterables.contains(link.getLinkTargets(), c4));
    }

    @Test
    public void testRemoveLinkTarget() throws Exception {
        Link link = manager.getEntity("link1", Link.class);
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        assertTrue(Iterables.contains(link.getLinkTargets(), c1));
        link.removeLinkTarget(c1);
        assertFalse(Iterables.contains(link.getLinkTargets(), c1));
    }

    @Test
    public void testGetLinkBodies() throws Exception {
        Link link = manager.getEntity("link2", Link.class);
        AccessPoint ur1 = manager.getEntity("ur1", AccessPoint.class);
        assertTrue(Iterables.contains(link.getLinkBodies(), ur1));
    }
}
