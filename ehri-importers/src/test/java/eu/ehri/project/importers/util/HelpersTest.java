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

package eu.ehri.project.importers.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class HelpersTest {
    @Test
    public void testIso639DashTwoCode() throws Exception {
        // two-to-three
        assertEquals("sqi", Helpers.iso639DashTwoCode("sq"));
        // bibliographic to term
        assertEquals("sqi", Helpers.iso639DashTwoCode("alb"));
        // name to code
        // FIXME fails when executed on a server with a Dutch locale
        assertEquals("eng", Helpers.iso639DashTwoCode("English"));
    }

    @Test
    public void testIso639DashOneCode() throws Exception {
        assertEquals("en", Helpers.iso639DashOneCode("eng"));
        assertEquals("cs", Helpers.iso639DashOneCode("ces"));
        assertEquals("cs", Helpers.iso639DashOneCode("cze"));
        assertEquals("sq", Helpers.iso639DashOneCode("sqi"));
        assertEquals("en", Helpers.iso639DashOneCode("English"));
        assertEquals("en-Latn", Helpers.iso639DashOneCode("eng-Latn"));
        assertEquals("en", Helpers.iso639DashOneCode("eng-"));
        assertEquals("---", Helpers.iso639DashOneCode("---"));
    }
}
