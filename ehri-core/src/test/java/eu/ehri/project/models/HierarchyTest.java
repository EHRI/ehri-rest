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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.ModelTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HierarchyTest extends ModelTestBase {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testCollectionHierarchy() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getEntity("c2", DocumentaryUnit.class);
        assertTrue(toList(c1.getChildren()).contains(c2));

        // check reverse
        assertEquals(c2.getParent(), c1);
    }

    @Test
    public void testCollectionAncestry() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getEntity("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getEntity("c3", DocumentaryUnit.class);
        // should be the first ancestor of c2
        assertEquals(toList(c2.getAncestors()).get(0), (c1));

        // and an ancestor of c3
        assertTrue(toList(c3.getAncestors()).contains(c1));
    }

    @Test
    public void testFullAncestry() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getEntity("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getEntity("c3", DocumentaryUnit.class);

        List<DocumentaryUnit> ancestors = toList(c3.getAncestors());
        assertEquals(2, ancestors.size());
        assertEquals(ancestors.get(0), c2);
        assertEquals(ancestors.get(1), c1);
    }
}
