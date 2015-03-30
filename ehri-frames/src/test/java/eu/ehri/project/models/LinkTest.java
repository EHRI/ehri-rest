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
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LinkTest extends AbstractFixtureTest {
    @Test
    public void testGetLinker() throws Exception {
        Link link = manager.getFrame("link1", Link.class);
        assertEquals(validUser, link.getLinker());
    }

    @Test
    public void testGetLinkTargets() throws Exception {
        Link link = manager.getFrame("link1", Link.class);
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        assertTrue(Iterables.contains(link.getLinkTargets(), c1));
        assertTrue(Iterables.contains(link.getLinkTargets(), c4));
    }

    @Test
    public void testGetLinkBodies() throws Exception {
        Link link = manager.getFrame("link2", Link.class);
        UndeterminedRelationship ur1 = manager.getFrame("ur1", UndeterminedRelationship.class);
        assertTrue(Iterables.contains(link.getLinkBodies(), ur1));
    }
}
