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

package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class SystemScopeTest {
    @Test
    public void testGetId() throws Exception {
        assertEquals(Entities.SYSTEM,
                SystemScope.getInstance().getId());
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(Entities.SYSTEM,
                SystemScope.getInstance().getType());
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(Entities.SYSTEM, SystemScope.getInstance().getType());
    }

    @Test
    public void testAsVertex() throws Exception {
        assertNull(SystemScope.getInstance().asVertex());
    }

    @Test
    public void testGetPermissionGrants() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getPermissionGrants()));
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getPermissionScopes()));
    }

    @Test
    public void testGetContainedItems() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getContainedItems()));
    }

    @Test
    public void testGetAllContainedItems() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getAllContainedItems()));
    }

    @Test
    public void testIdPath() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope.getInstance().idPath()));
    }
}
