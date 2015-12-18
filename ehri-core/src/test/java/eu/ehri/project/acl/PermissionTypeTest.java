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

import org.junit.Test;

import static eu.ehri.project.acl.PermissionType.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: mike
 */
public class PermissionTypeTest {
    @Test
    public void testContains() throws Exception {
        // Sanity checking...
        assertTrue(OWNER.contains(CREATE));
        assertTrue(OWNER.contains(UPDATE));
        assertTrue(OWNER.contains(DELETE));
        assertTrue(OWNER.contains(ANNOTATE));
        assertFalse(OWNER.contains(GRANT));

        assertFalse(CREATE.contains(UPDATE));
        assertFalse(CREATE.contains(OWNER));
    }
}
