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
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CountryTest extends AbstractFixtureTest {
    @Test
    public void testGetChildCount() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        Repository repo = new BundleManager(graph)
                .create(Bundle.fromData(TestData.getTestAgentBundle()), Repository.class);
        country.addRepository(repo);
        // 2 nl repositories in the fixtures, plus the one we just made...
        assertEquals(3, country.countChildren());
    }

    @Test
    public void testGetChildCountOnDeletion() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        assertEquals(2, country.countChildren());
        api(validUser).delete("r1");
        assertEquals(1, country.countChildren());
    }

    @Test
    public void testGetRepositories() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        assertEquals(2, Iterables.size(country.getRepositories()));
    }

    @Test
    public void testGetTopLevelDocumentaryUnits() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        // Expecting c1, c4, and m1-19
        assertEquals(3, Iterables.size(country.getTopLevelDocumentaryUnits()));
    }

    @Test
    public void testAddRepository() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        Repository repo = new BundleManager(graph)
                .create(Bundle.fromData(TestData.getTestAgentBundle()), Repository.class);
        // Test setting country on repo delegates correctly and
        // increments the country count...
        repo.setCountry(country);
        // 2 nl repositories in the fixtures, plus the one we just made...
        assertEquals(3, country.countChildren());
    }
}
