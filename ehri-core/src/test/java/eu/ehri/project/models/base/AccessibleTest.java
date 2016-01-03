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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class AccessibleTest extends AbstractFixtureTest {
    @Test
    public void testGetAccessors() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        Accessible admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Accessible.class);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(2L, accessors.size());
        assertTrue(accessors.contains(validUser)); // mike
        assertTrue(accessors.contains(admin));
    }

    @Test
    public void testAddAccessor() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(2L, accessors.size());
        c1.addAccessor(admin);
        assertEquals(2L, Iterables.size(c1.getAccessors())); // same size
        c1.addAccessor(invalidUser);
        assertEquals(3L, Iterables.size(c1.getAccessors()));
    }

    @Test
    public void testRemoveAccessor() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        c1.removeAccessor(admin);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(1L, accessors.size());
        assertTrue(accessors.contains(validUser));
    }

    @Test
    public void testGetPermissionScope() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        PermissionScope r1 = manager.getEntity("r1", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
    }

    @Test
    public void testSetPermissionScope() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        PermissionScope r1 = manager.getEntity("r1", PermissionScope.class);
        PermissionScope r2 = manager.getEntity("r2", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
        c1.setPermissionScope(r2);
        assertEquals(r2, c1.getPermissionScope());
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        Accessible c1 = manager.getEntity("c1", Accessible.class);
        Accessible c2 = manager.getEntity("c2", Accessible.class);
        PermissionScope r1 = manager.getEntity("r1", PermissionScope.class);
        PermissionScope nl = manager.getEntity("nl", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
        List<PermissionScope> scopes = Lists.newArrayList(c2.getPermissionScopes());
        assertEquals(3L, scopes.size());
        assertTrue(scopes.contains(c1));
        assertTrue(scopes.contains(r1));
        assertTrue(scopes.contains(nl));
    }

    @Test
    public void testGetHistory() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        Mutation<DocumentaryUnit> update = doUpdate(c1);
        assertTrue(update.updated());
        Iterable<SystemEvent> history = c1.getHistory();
        assertEquals(1L, Iterables.size(history));
    }

    @Test
    public void testGetLatestEvent() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        assertNull(c1.getLatestEvent());
        doUpdate(c1);
        assertNotNull(c1.getLatestEvent());
    }

    @Test
    public void testHasAccessRestrictions() throws Exception {
        Accessible ann3 = manager.getEntity("ann3", Accessible.class);
        // Because ann4 is promoted, access is unrestricted.
        Accessible ann4 = manager.getEntity("ann4", Accessible.class);
        assertTrue(ann3.hasAccessRestriction());
        assertFalse(ann4.hasAccessRestriction());
    }

    private Mutation<DocumentaryUnit> doUpdate(DocumentaryUnit unit) throws Exception {
        Bundle doc = new Serializer(graph).entityToBundle(unit)
                .withDataValue("somekey", "someval");
        Crud<DocumentaryUnit> crud = ViewFactory.getCrudWithLogging(graph, DocumentaryUnit.class);
        return crud.update(doc, validUser);
    }
}
