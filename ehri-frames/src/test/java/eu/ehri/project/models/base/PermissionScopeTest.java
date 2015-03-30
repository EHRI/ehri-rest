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

package eu.ehri.project.models.base;

import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PermissionScopeTest extends AbstractFixtureTest {
    public DocumentaryUnit doc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        doc = manager.getFrame("c2", DocumentaryUnit.class);
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        List<PermissionScope> scopes = Lists.newArrayList(
                manager.getFrame("c1", PermissionScope.class),
                manager.getFrame("r1", PermissionScope.class),
                manager.getFrame("nl", PermissionScope.class));
        assertEquals(scopes, Iterables.toList(doc.getPermissionScopes()));
    }

    @Test
    public void testContainedItems() throws Exception {
        PermissionScope c1 = manager.getFrame("c1", PermissionScope.class);
        PermissionScope c2 = manager.getFrame("c2", PermissionScope.class);
        Iterable<AccessibleEntity> containedItems = c1.getContainedItems();
        assertEquals(1L, Iterables.count(containedItems));
        assertEquals(c2, containedItems.iterator().next());
    }

    @Test
    public void testAllContainedItems() throws Exception {
        PermissionScope r1 = manager.getFrame("r1", PermissionScope.class);
        PermissionScope c1 = manager.getFrame("c1", PermissionScope.class);
        PermissionScope c2 = manager.getFrame("c2", PermissionScope.class);
        PermissionScope c3 = manager.getFrame("c3", PermissionScope.class);
        PermissionScope c4 = manager.getFrame("c4", PermissionScope.class);
        List<AccessibleEntity> r1contained = Lists.newArrayList(r1.getAllContainedItems());
        assertEquals(5L, r1contained.size());
        assertTrue(r1contained.contains(manager.cast(c1, AccessibleEntity.class)));
        assertTrue(r1contained.contains(manager.cast(c2, AccessibleEntity.class)));
        assertTrue(r1contained.contains(manager.cast(c3, AccessibleEntity.class)));
        assertTrue(r1contained.contains(manager.cast(c4, AccessibleEntity.class)));

        List<AccessibleEntity> c1contained = Lists.newArrayList(c1.getAllContainedItems());
        assertEquals(2L, c1contained.size());
        assertTrue(c1contained.contains(manager.cast(c2, AccessibleEntity.class)));
        assertTrue(c1contained.contains(manager.cast(c3, AccessibleEntity.class)));
    }

    @Test
    public void testIdChain() throws Exception {
        assertEquals(Lists.newArrayList("nl", "r1", "c1", "c2"), doc.idPath());
    }

    @Test
    public void testIdentifierIdRelationships() throws Exception {

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        Repository repo = manager.getFrame("r1", Repository.class);
        BundleDAO dao = new BundleDAO(graph, repo.idPath());
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        assertEquals("nl-r1-someid_01", doc.getId());
        doc.setPermissionScope(repo);
        assertEquals(Lists.newArrayList("nl", "r1", "someid-01"), doc.idPath());
    }
}
