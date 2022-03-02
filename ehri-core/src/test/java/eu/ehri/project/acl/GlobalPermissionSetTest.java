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


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobalPermissionSetTest {

    @Test
    public void testEquals() throws Exception {
        GlobalPermissionSet.Builder a = GlobalPermissionSet.newBuilder();
        GlobalPermissionSet.Builder b = GlobalPermissionSet.newBuilder();

        a.set(ContentTypes.COUNTRY, PermissionType.CREATE, PermissionType.UPDATE);
        b.set(ContentTypes.COUNTRY, PermissionType.UPDATE, PermissionType.CREATE);
        GlobalPermissionSet setA = a.build();
        GlobalPermissionSet setB = b.build();
        // Test ordering doesn't matter
        assertEquals(setA, setB);
        assertTrue(setA.has(ContentTypes.COUNTRY, PermissionType.CREATE));
        assertTrue(setA.has(ContentTypes.COUNTRY, PermissionType.UPDATE));
        assertFalse(setA.has(ContentTypes.COUNTRY, PermissionType.DELETE));
    }
}
