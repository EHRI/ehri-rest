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

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class InheritedGlobalPermissionSetTest extends AbstractFixtureTest {

    private AclManager aclManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        aclManager = new AclManager(graph);
    }

    @Test
    public void testHas() throws Exception {
        InheritedGlobalPermissionSet permissions1
                = aclManager.getInheritedGlobalPermissions(invalidUser);
        assertFalse(permissions1.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE));
        assertFalse(permissions1.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE));

        InheritedGlobalPermissionSet permissions2
                = aclManager.getInheritedGlobalPermissions(validUser);
        assertTrue(permissions2.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE));
        assertTrue(permissions2.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE));
    }

    @Test
    public void testSerialize() throws Exception {
        InheritedGlobalPermissionSet permissions
                = aclManager.getInheritedGlobalPermissions(invalidUser);
        List<Map<String,GlobalPermissionSet>> list = permissions.serialize();
        assertEquals(2, list.size());
    }
}
