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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DatePeriod;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.importers.util.DateParser.END_STANDARD_DATE;
import static eu.ehri.project.importers.util.DateParser.START_STANDARD_DATE;
import static eu.ehri.project.importers.util.DateParser.extractDates;
import static eu.ehri.project.importers.util.DateParser.normaliseDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises {@link DateParser} through its three date sources, in the order they're
 * tried by {@link DateParser#extractDates}:
 * <ol>
 *     <li>a structured {@code DatePeriod} sub-node (EAD3's {@code unitdatestructured},
 *     EAC's {@code existDates/dateRange})</li>
 *     <li>EAD2002's {@code unitdate/@normal} attribute, reconciled against the parallel
 *     {@code unitDates} text</li>
 *     <li>free-text pattern matching against {@code unitDates}/{@code creationDate}/
 *     {@code existDate} (or any other property registered in {@code dates.properties})</li>
 * </ol>
 * Whichever source a date comes from, the result is the same shape: {@code startDate}/
 * {@code endDate} always widened to a full {@code YYYY-MM-DD}, with the true
 * granularity carried separately in {@code precision}.
 */
public class DateParserTest {

    // --- Structured DatePeriod sub-node (Entities.DATE_PERIOD) ------------------

    @Test
    public void structuredSubNode_mapOrListOfMapsAreAllExtracted() {
        Map<String, Object> single = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "1939", Ontology.DATE_PERIOD_END_DATE, "1945"));
        Map<String, Object> period = onlyExtracted(single);
        assertEquals("1939-01-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1945-12-31", period.get(Ontology.DATE_PERIOD_END_DATE));

        Map<String, Object> list = itemDataWith(Entities.DATE_PERIOD, Lists.newArrayList(
                ImmutableMap.of(Ontology.DATE_PERIOD_START_DATE, "1920", Ontology.DATE_PERIOD_END_DATE, "1940"),
                ImmutableMap.of(Ontology.DATE_PERIOD_START_DATE, "1941", Ontology.DATE_PERIOD_END_DATE, "1950")));
        assertEquals(2, extractDates(list).size());
    }

    @Test
    public void structuredSubNode_unrecognisedShapeIsSkippedButConsumed() {
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, "not a map or list");
        assertTrue(extractDates(data).isEmpty());
        assertFalse(data.containsKey(Entities.DATE_PERIOD));
    }

    @Test
    public void structuredSubNode_standardDateAttributesAreMovedAndWiden() {
        // Simulates EAD3's fromdate/@standarddate, captured into these temp keys.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                START_STANDARD_DATE, "1939-04", END_STANDARD_DATE, "1945-06"));

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("1939-04-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1945-06-30", period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(DatePeriod.DatePrecision.month.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
        assertFalse(period.containsKey(START_STANDARD_DATE));
    }

    @Test
    public void structuredSubNode_explicitPrecisionOverridesInference() {
        // @localtype="quarter": ISO 8601 can't express it, so without this the
        // month-truncated @standarddate alone would infer "month" instead.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                START_STANDARD_DATE, "1939-04", END_STANDARD_DATE, "1945-06",
                Ontology.DATE_PERIOD_PRECISION, "quarter"));

        assertEquals(DatePeriod.DatePrecision.quarter.name(),
                onlyExtracted(data).get(Ontology.DATE_PERIOD_PRECISION));
    }

    @Test
    public void structuredSubNode_unparseableDateIsKeptAsIs() {
        // No fallback for a structured sub-node, unlike the text-driven sources
        // below, so a bad value is left untouched rather than dropped.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD,
                ImmutableMap.of(Ontology.DATE_PERIOD_START_DATE, "circa 1939"));

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("circa 1939", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertNull(period.get(Ontology.DATE_PERIOD_PRECISION));
    }

    // --- EAD2002 unitdate/@normal, reconciled against unitDates text ------------

    @Test
    public void normalAttribute_isParsedIntoStartEndAndPrecision() {
        // Single date: no end, widened, year precision.
        assertNormalPeriod("1933", "1933", "1933-01-01", null, DatePeriod.DatePrecision.year);
        // Range, hyphenated: month precision.
        assertNormalPeriod("1939-1945", "1939-04/1945-06", "1939-04-01", "1945-06-30", DatePeriod.DatePrecision.month);
        // Range, compact (the form Ead2002Exporter itself writes): day precision.
        assertNormalPeriod("1940-1944", "19400101/19441231", "1940-01-01", "1944-12-31", DatePeriod.DatePrecision.day);
    }

    @Test
    public void normalAttribute_multipleUnitdatesArePositionallyPaired() {
        Map<String, Object> data = itemDataWith("unitDates", Lists.newArrayList("1939-1945", "1950"));
        data.put("unitDatesNormal", Lists.newArrayList("1939/1945", "1950"));

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(2, extracted.size());
        assertTrue(extracted.stream().anyMatch(p -> "1939-1945".equals(p.get(Ontology.DATE_HAS_DESCRIPTION))));
        assertTrue(extracted.stream().anyMatch(p -> "1950".equals(p.get(Ontology.DATE_HAS_DESCRIPTION))));
    }

    @Test
    public void normalAttribute_mismatchedListLengthsFallBackToPatternMatching() {
        // Two unitdates but one @normal: not safe to say which text it belongs to,
        // so normal-attribute resolution is skipped entirely and both texts go
        // through pattern matching instead - which still recovers "1939-1945".
        Map<String, Object> data = itemDataWith("unitDates", Lists.newArrayList("1939-1945", "circa 1978"));
        data.put("unitDatesNormal", Lists.newArrayList("1939/1945"));

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(1, extracted.size());
        assertEquals("1939-1945", extracted.get(0).get(Ontology.DATE_HAS_DESCRIPTION));
        assertEquals("circa 1978", data.get("unitDates"));
    }

    @Test
    public void normalAttribute_invalidValueFallsBackAndIsPreserved() {
        Map<String, Object> data = itemDataWith("unitDates", "not a real date");
        data.put("unitDatesNormal", "garbage");

        assertTrue(extractDates(data).isEmpty());
        assertEquals("not a real date", data.get("unitDates"));
    }

    @Test
    public void normalAttribute_temporaryKeyIsNeverPersisted() {
        Map<String, Object> matched = itemDataWith("unitDates", "1933");
        matched.put("unitDatesNormal", "1933");
        extractDates(matched);
        assertFalse(matched.containsKey("unitDatesNormal"));

        Map<String, Object> mismatched = itemDataWith("unitDates", Lists.newArrayList("1933", "1934"));
        mismatched.put("unitDatesNormal", "1933");
        extractDates(mismatched);
        assertFalse(mismatched.containsKey("unitDatesNormal"));
    }

    // --- Free-text pattern matching ----------------------------------------------

    @Test
    public void patternMatching_recognisesKnownSloppyFormats() {
        assertPeriod("1944", "1944-01-01", "1944-12-31", DatePeriod.DatePrecision.year);
        assertPeriod("1939-1945", "1939-01-01", "1945-12-31", DatePeriod.DatePrecision.year);
        assertPeriod("[1924]", "1924-01-01", "1924-12-31", DatePeriod.DatePrecision.year);
        assertPeriod("1924-[1947]", "1924-01-01", "1947-12-31", DatePeriod.DatePrecision.year);
        // Yad Vashem / ICA-Atom style, non-zero-padded month/day.
        assertPeriod("1939-4-1 - 1945-6-30", "1939-04-01", "1945-06-30", DatePeriod.DatePrecision.day);
        // Bundesarchiv's 2-digit year-suffix shorthand for a range within a century.
        assertPeriod("1906/19", "1906-01-01", "1919-12-31", DatePeriod.DatePrecision.year);
    }

    @Test
    public void patternMatching_unmatchedTextIsPreservedUntouched() {
        Map<String, Object> data = itemDataWith("unitDates", "not a date at all");
        assertTrue(extractDates(data).isEmpty());
        assertEquals("not a date at all", data.get("unitDates"));
    }

    @Test
    public void patternMatching_commaSeparatedValues() {
        // Clean split: both segments match independently.
        Map<String, Object> clean = itemDataWith("unitDates", "1934,1978");
        assertEquals(2, extractDates(clean).size());
        assertFalse(clean.containsKey("unitDates"));

        // A space after the comma is enough to make " 1978" fail every pattern -
        // segments aren't trimmed before matching, only when preserved as leftovers.
        Map<String, Object> spaced = itemDataWith("unitDates", "1934, 1978");
        List<Map<String, Object>> extracted = extractDates(spaced);
        assertEquals(1, extracted.size());
        assertEquals("1934", extracted.get(0).get(Ontology.DATE_HAS_DESCRIPTION));
        assertEquals("1978", spaced.get("unitDates"));
    }

    @Test
    public void patternMatching_partiallyMatchedValuesLeaveTheRestAsText() {
        Map<String, Object> data = itemDataWith("unitDates", "1934, not parseable");
        assertEquals(1, extractDates(data).size());
        assertEquals("not parseable", data.get("unitDates"));
    }

    // --- DatePeriodType: only unitDates is implicitly a creation date -----------

    @Test
    public void datePeriodType_unitDatesIsCreationButOtherPropertiesAreNot() {
        Map<String, Object> data = itemDataWith("unitDates", "1934");
        data.put("existDate", "1900");

        List<Map<String, Object>> extracted = extractDates(data);

        Map<String, Object> unitDatePeriod = byDescription(extracted, "1934");
        Map<String, Object> existDatePeriod = byDescription(extracted, "1900");
        assertEquals(DatePeriod.DatePeriodType.creation.name(), unitDatePeriod.get(Ontology.DATE_PERIOD_TYPE));
        assertNull(existDatePeriod.get(Ontology.DATE_PERIOD_TYPE));
    }

    // --- Deduplication: exact-duplicate DatePeriods are collapsed to one --------

    @Test
    public void deduplication_collapsesExactRepeatsHoweverTheyArose() {
        // A repeated <unitdate normal="..."> element.
        Map<String, Object> viaNormal = itemDataWith("unitDates", Lists.newArrayList("1939-1945", "1939-1945"));
        viaNormal.put("unitDatesNormal", Lists.newArrayList("1939/1945", "1939/1945"));
        assertEquals(1, extractDates(viaNormal).size());

        // A repeated structured sub-node.
        Map<String, Object> period = ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "1939", Ontology.DATE_PERIOD_END_DATE, "1945");
        Map<String, Object> viaStructured = itemDataWith(Entities.DATE_PERIOD, Lists.newArrayList(period, period));
        assertEquals(1, extractDates(viaStructured).size());
    }

    @Test
    public void deduplication_entriesThatDifferAreBothKept() {
        Map<String, Object> first = ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "1939", Ontology.DATE_PERIOD_END_DATE, "1945",
                Ontology.DATE_HAS_DESCRIPTION, "Second World War");
        Map<String, Object> second = ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "1939", Ontology.DATE_PERIOD_END_DATE, "1945",
                Ontology.DATE_HAS_DESCRIPTION, "Nazi occupation");
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, Lists.newArrayList(first, second));

        assertEquals(2, extractDates(data).size());
    }

    // --- normaliseDate(): the widening primitive used by every source above -----

    @Test
    public void normaliseDate_widensToStartOrEndOfPeriod() {
        assertEquals("1944-01-01", normaliseDate("1944"));
        assertEquals("1944-01-01", normaliseDate("1944-01"));
        assertEquals("1944-12-31", normaliseDate("1944", true));
        assertEquals("1944-01-31", normaliseDate("1944-01", true));
    }

    @Test
    public void normaliseDate_fullySpecifiedDatesAreUnchanged() {
        // Including when not zero-padded, which previously fooled the lenient
        // "is this actually just a year-month?" check into partially matching
        // the "yyyy-MM" prefix and incorrectly widening it anyway.
        assertEquals("1944-06-15", normaliseDate("1944-06-15", true));
        assertEquals("1944-06-15", normaliseDate("1944-6-15", true));
    }

    // --- helpers ------------------------------------------------------------

    private static Map<String, Object> itemDataWith(String key, Object value) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(key, value);
        return data;
    }

    private static Map<String, Object> onlyExtracted(Map<String, Object> data) {
        List<Map<String, Object>> extracted = extractDates(data);
        assertEquals(1, extracted.size());
        return extracted.get(0);
    }

    private static Map<String, Object> byDescription(List<Map<String, Object>> periods, String description) {
        return periods.stream().filter(p -> description.equals(p.get(Ontology.DATE_HAS_DESCRIPTION)))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static void assertPeriod(String raw, String start, String end, DatePeriod.DatePrecision precision) {
        Map<String, Object> period = onlyExtracted(itemDataWith("unitDates", raw));
        assertEquals(raw, start, period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals(raw, end, period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(raw, precision.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
    }

    private static void assertNormalPeriod(String unitDate, String normal, String start, String end,
            DatePeriod.DatePrecision precision) {
        Map<String, Object> data = itemDataWith("unitDates", unitDate);
        data.put("unitDatesNormal", normal);
        Map<String, Object> period = onlyExtracted(data);
        assertEquals(normal, start, period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals(normal, end, period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(normal, precision.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
    }
}
