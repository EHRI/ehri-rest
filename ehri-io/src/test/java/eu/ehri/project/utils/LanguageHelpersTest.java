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

package eu.ehri.project.utils;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;


public class LanguageHelpersTest {

    @Test
    public void testIso639DashTwoCode() {
        // two-to-three
        assertEquals("sqi", LanguageHelpers.iso639DashTwoCode("sq"));
        // bibliographic to term
        assertEquals("sqi", LanguageHelpers.iso639DashTwoCode("alb"));
        // name to code
        // FIXME fails when executed on a server with a Dutch locale
        assertEquals("eng", LanguageHelpers.iso639DashTwoCode("English"));
    }

    @Test
    public void testConvertCode() {
        assertEquals(Optional.of("sqi"), LanguageHelpers.convertCode("sq"));
        assertEquals(Optional.of("sqi"), LanguageHelpers.convertCode("alb"));
        assertEquals(Optional.of("eng"), LanguageHelpers.convertCode("en"));
        assertEquals(Optional.empty(), LanguageHelpers.convertCode(null));
        assertEquals(Optional.empty(), LanguageHelpers.convertCode("unknown"));
    }

    @Test
    public void testIso639DashOneCode() {
        assertEquals("en", LanguageHelpers.iso639DashOneCode("eng"));
        assertEquals("cs", LanguageHelpers.iso639DashOneCode("ces"));
        assertEquals("cs", LanguageHelpers.iso639DashOneCode("cze"));
        assertEquals("sq", LanguageHelpers.iso639DashOneCode("sqi"));
        assertEquals("en", LanguageHelpers.iso639DashOneCode("English"));
        assertEquals("en-Latn", LanguageHelpers.iso639DashOneCode("eng-Latn"));
        assertEquals("en", LanguageHelpers.iso639DashOneCode("eng-"));
        assertEquals("---", LanguageHelpers.iso639DashOneCode("---"));
    }

    @Test
    public void testCountryCodeToContinent() {
        Optional<String> c1 = LanguageHelpers.countryCodeToContinent("gb");
        assertTrue(c1.isPresent());
        assertEquals("Europe", c1.get());

        Optional<String> c2 = LanguageHelpers.countryCodeToContinent("us");
        assertTrue(c2.isPresent());
        assertEquals("North America", c2.get());

        Optional<String> c3 = LanguageHelpers.countryCodeToContinent("nz");
        assertTrue(c3.isPresent());
        assertEquals("Australia", c3.get());
    }

    @Test
    public void testCodeToName() {
        assertEquals("English", LanguageHelpers.codeToName("eng"));
        assertEquals("German", LanguageHelpers.codeToName("deu"));
        assertEquals("German", LanguageHelpers.codeToName("ger"));
    }

    @Test
    public void countryCodeToName() {
        assertEquals("Germany", LanguageHelpers.countryCodeToName("de"));
        assertEquals("United Kingdom", LanguageHelpers.countryCodeToName("gb"));
        assertEquals("France", LanguageHelpers.countryCodeToName("fr"));
        assertEquals("Kosovo", LanguageHelpers.countryCodeToName("xk"));
    }
}