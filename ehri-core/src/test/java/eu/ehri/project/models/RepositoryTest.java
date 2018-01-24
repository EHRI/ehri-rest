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
import static org.junit.Assert.assertTrue;


public class RepositoryTest extends AbstractFixtureTest {
    @Test
    public void testGetCollections() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        assertEquals(3, Iterables.size(r1.getTopLevelDocumentaryUnits()));

        // Check the cached size
        assertEquals(3, r1.countChildren());
    }

    @Test
    public void testRepositoryCanGetAllCollections() throws ItemNotFound {
        Repository agent = manager.getEntity("r1", Repository.class);
        assertEquals(3, Iterables.size(agent.getTopLevelDocumentaryUnits()));
        assertEquals(5, Iterables.size(agent.getAllDocumentaryUnits()));

    }

    @Test
    public void testRepositoryDescription() throws ItemNotFound {
        RepositoryDescription rd1 = manager.getEntity("rd1", RepositoryDescription.class);
        Address ar1 = manager.getEntity("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
