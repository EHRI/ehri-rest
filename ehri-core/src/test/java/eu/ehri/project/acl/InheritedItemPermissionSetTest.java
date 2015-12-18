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

import eu.ehri.project.models.Repository;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class InheritedItemPermissionSetTest extends AbstractFixtureTest {

    private AclManager aclManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        aclManager = new AclManager(graph);
    }

    @Test
    public void testHas() throws Exception {
        Repository r2 = manager.getFrame("r2", Repository.class);
        InheritedItemPermissionSet permissionSet
                = aclManager.getInheritedItemPermissions(r2, invalidUser);
        assertFalse(permissionSet.has(PermissionType.CREATE));
        assertTrue(permissionSet.has(PermissionType.UPDATE));
        assertFalse(permissionSet.has(PermissionType.DELETE));
    }

    @Test
    public void testSerialize() throws Exception {
        InheritedItemPermissionSet permissions
                = aclManager.getInheritedItemPermissions(manager.getFrame("r2", Repository.class), invalidUser
        );
        List<Map<String,List<PermissionType>>> serialized = permissions.serialize();
        // Should contain Reto and Kcl
        assertEquals(2L, serialized.size());
    }
}
