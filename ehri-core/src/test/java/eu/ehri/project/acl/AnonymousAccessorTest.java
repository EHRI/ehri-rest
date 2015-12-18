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

package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Group;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AnonymousAccessorTest {
    @Test
    public void testIsAdmin() throws Exception {
        assertFalse(AnonymousAccessor.getInstance().isAdmin());
    }

    @Test
    public void testIsAnonymous() throws Exception {
        assertTrue(AnonymousAccessor.getInstance().isAnonymous());
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(
                Group.ANONYMOUS_GROUP_IDENTIFIER,
                AnonymousAccessor.getInstance().getId());
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(Entities.GROUP,
                AnonymousAccessor.getInstance().getType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAsVertex() throws Exception {
        AnonymousAccessor.getInstance().asVertex();
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(
                Group.ANONYMOUS_GROUP_IDENTIFIER,
                AnonymousAccessor.getInstance().getIdentifier());
    }

    @Test
    public void testGetParents() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getParents()));
    }

    @Test
    public void testGetAllParents() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getAllParents()));
    }

    @Test
    public void testGetPermissionGrants() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getPermissionGrants()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddPermissionGrant() throws Exception {
        AnonymousAccessor.getInstance().addPermissionGrant(null);
    }
}
