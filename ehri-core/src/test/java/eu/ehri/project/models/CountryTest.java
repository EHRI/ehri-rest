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
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import eu.ehri.project.views.ViewFactory;
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
        assertEquals(3L, country.getChildCount());
    }

    @Test
    public void testGetChildCountOnDeletion() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        Repository repo = manager.getEntity("r1", Repository.class);
        assertEquals(2L, country.getChildCount());
        ViewFactory.getCrudNoLogging(graph, Repository.class).delete("r1", validUser);
        assertEquals(1L, country.getChildCount());
    }

    @Test
    public void testGetRepositories() throws Exception {
        Country country = manager.getEntity("nl", Country.class);
        assertEquals(2L, Iterables.size(country.getRepositories()));
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
        assertEquals(3L, country.getChildCount());
    }
}
