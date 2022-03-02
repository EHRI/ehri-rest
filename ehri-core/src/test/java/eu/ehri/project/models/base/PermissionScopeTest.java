/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.models.base;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PermissionScopeTest extends AbstractFixtureTest {
    public DocumentaryUnit doc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        doc = manager.getEntity("c2", DocumentaryUnit.class);
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        List<PermissionScope> scopes = Lists.newArrayList(
                manager.getEntity("c1", PermissionScope.class),
                manager.getEntity("r1", PermissionScope.class),
                manager.getEntity("nl", PermissionScope.class));
        assertEquals(scopes, Lists.newArrayList(doc.getPermissionScopes()));
    }

    @Test
    public void testContainedItems() throws Exception {
        PermissionScope c1 = manager.getEntity("c1", PermissionScope.class);
        PermissionScope c2 = manager.getEntity("c2", PermissionScope.class);
        Iterable<Accessible> containedItems = c1.getContainedItems();
        assertEquals(1, Iterables.size(containedItems));
        assertEquals(c2, containedItems.iterator().next());
    }

    @Test
    public void countContainedItems() throws Exception {
        PermissionScope c1 = manager.getEntity("c1", PermissionScope.class);
        assertEquals(1, c1.countContainedItems());
    }

    @Test
    public void testAllContainedItems() throws Exception {
        PermissionScope r1 = manager.getEntity("r1", PermissionScope.class);
        PermissionScope c1 = manager.getEntity("c1", PermissionScope.class);
        PermissionScope c2 = manager.getEntity("c2", PermissionScope.class);
        PermissionScope c3 = manager.getEntity("c3", PermissionScope.class);
        PermissionScope c4 = manager.getEntity("c4", PermissionScope.class);
        List<Accessible> r1contained = Lists.newArrayList(r1.getAllContainedItems());
        assertEquals(5, r1contained.size());
        assertTrue(r1contained.contains(c1.as(Accessible.class)));
        assertTrue(r1contained.contains(c2.as(Accessible.class)));
        assertTrue(r1contained.contains(c3.as(Accessible.class)));
        assertTrue(r1contained.contains(c4.as(Accessible.class)));

        List<Accessible> c1contained = Lists.newArrayList(c1.getAllContainedItems());
        assertEquals(2, c1contained.size());
        assertTrue(c1contained.contains(c2.as(Accessible.class)));
        assertTrue(c1contained.contains(c3.as(Accessible.class)));
    }

    @Test
    public void testIdChain() throws Exception {
        assertEquals(Lists.newArrayList("nl", "r1", "c1", "c2"), doc.idPath());
    }

    @Test
    public void testIdentifierIdRelationships() throws Exception {

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        Repository repo = manager.getEntity("r1", Repository.class);
        BundleManager dao = new BundleManager(graph, repo.idPath());
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        assertEquals("nl-r1-someid_01", doc.getId());
        doc.setPermissionScope(repo);
        assertEquals(Lists.newArrayList("nl", "r1", "someid-01"), doc.idPath());
    }
}
