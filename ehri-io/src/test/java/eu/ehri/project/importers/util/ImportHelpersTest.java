/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.util;

import com.google.common.collect.Maps;
import eu.ehri.project.models.EntityClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ImportHelpersTest {

    @Test
    public void putPropertyInGraphNormalisesLanguageOfMaterial() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "English");
        assertEquals("eng", c.get("languageOfMaterial"));
        assertFalse(c.containsKey("languageOfMaterialNotes"));
    }

    @Test
    public void putPropertyInGraphDivertsUnnormalisableLanguageOfMaterialToNotes() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "In het Frans");
        assertFalse(c.containsKey("languageOfMaterial"));
        assertEquals("In het Frans", c.get("languageOfMaterialNotes"));
    }

    @Test
    public void putPropertyInGraphKeepsValidAndInvalidLanguageOfMaterialValuesSeparate() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "eng");
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "In het Frans");
        assertEquals("eng", c.get("languageOfMaterial"));
        assertEquals("In het Frans", c.get("languageOfMaterialNotes"));
    }

    @Test
    public void putPropertyInGraphAccumulatesMultipleUnnormalisableValuesAsList() {
        // Like any other non-multivalued property, repeated raw values accumulate as a
        // list at this stage; they are only joined into a single string once flattened
        // (see flattenNonMultivaluedPropertiesJoinsMultipleLanguageOfMaterialNotes below).
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "In het Frans");
        ImportHelpers.putPropertyInGraph(c, "languageOfMaterial", "In het Engels");
        assertFalse(c.containsKey("languageOfMaterial"));
        assertEquals(Arrays.asList("In het Frans", "In het Engels"), c.get("languageOfMaterialNotes"));
    }

    @Test
    public void flattenNonMultivaluedPropertiesJoinsMultipleLanguageOfMaterialNotes() {
        Object flattened = ImportHelpers.flattenNonMultivaluedProperties("languageOfMaterialNotes",
                Arrays.asList("In het Frans", "In het Engels"), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION);
        assertEquals("In het Frans\n\nIn het Engels", flattened);
    }

    @Test
    public void flattenNonMultivaluedPropertiesDedupesMultivaluedLists() {
        // languageOfMaterial is multivalued: an exact repeat (e.g. from a source document
        // that redundantly repeats the same language) is collapsed rather than kept.
        Object flattened = ImportHelpers.flattenNonMultivaluedProperties("languageOfMaterial",
                Arrays.asList("eng", "fra", "eng"), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION);
        assertEquals(Arrays.asList("eng", "fra"), flattened);
    }

    @Test
    public void flattenNonMultivaluedPropertiesDedupesBeforeJoiningNonMultivaluedLists() {
        // scopeAndContent is not multivalued: duplicate entries are still collapsed
        // before the remainder are joined into a single string.
        Object flattened = ImportHelpers.flattenNonMultivaluedProperties("scopeAndContent",
                Arrays.asList("some text", "some text", "other text"), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION);
        assertEquals("some text\n\nother text", flattened);
    }

    @Test
    public void putPropertyInGraphNormalisesLanguageCode() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageCode", "dut");
        assertEquals("nld", c.get("languageCode"));
    }

    @Test
    public void putPropertyInGraphKeepsUnnormalisableLanguageCodeAsIs() {
        // languageCode has no fallback property configured, so an unrecognised value
        // is kept as-is, matching the behaviour of a property with no normaliser at all.
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "languageCode", "not a real code");
        assertEquals("not a real code", c.get("languageCode"));
    }

    @Test
    public void putPropertyInGraphNormalisesScriptOfMaterial() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "scriptOfMaterial", "Latin");
        assertEquals("Latn", c.get("scriptOfMaterial"));
    }

    @Test
    public void putPropertyInGraphStoresUnconfiguredPropertiesAsPlainText() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.putPropertyInGraph(c, "scopeAndContent", "  some   text  ");
        assertEquals("some text", c.get("scopeAndContent"));
    }

    @Test
    public void overwritePropertyInGraphNormalisesLanguageOfMaterial() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.overwritePropertyInGraph(c, "languageOfMaterial", "English");
        assertEquals("eng", c.get("languageOfMaterial"));
        assertFalse(c.containsKey("languageOfMaterialNotes"));
    }

    @Test
    public void overwritePropertyInGraphHonoursFallbackProperty() {
        // Matches putPropertyInGraph's behaviour: an unnormalisable languageOfMaterial
        // value is diverted to languageOfMaterialNotes, not left under languageOfMaterial.
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.overwritePropertyInGraph(c, "languageOfMaterial", "In het Frans");
        assertFalse(c.containsKey("languageOfMaterial"));
        assertEquals("In het Frans", c.get("languageOfMaterialNotes"));
    }

    @Test
    public void overwritePropertyInGraphReplacesRatherThanAccumulates() {
        Map<String, Object> c = Maps.newHashMap();
        ImportHelpers.overwritePropertyInGraph(c, "scopeAndContent", "first");
        ImportHelpers.overwritePropertyInGraph(c, "scopeAndContent", "second");
        assertEquals("second", c.get("scopeAndContent"));
    }
}
