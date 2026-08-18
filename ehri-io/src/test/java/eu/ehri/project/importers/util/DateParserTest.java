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
 * Whichever source a date comes from, the end result is the same shape: a
 * {@code startDate}/{@code endDate} always widened to a full {@code YYYY-MM-DD}, with
 * the true granularity carried separately in {@code precision}.
 */
public class DateParserTest {

    // --- Structured DatePeriod sub-node (Entities.DATE_PERIOD) ------------------

    @Test
    public void structuredSubNode_singleMapIsExtracted() {
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "1939",
                Ontology.DATE_PERIOD_END_DATE, "1945"));

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(1, extracted.size());
        assertEquals("1939-01-01", extracted.get(0).get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1945-12-31", extracted.get(0).get(Ontology.DATE_PERIOD_END_DATE));
    }

    @Test
    public void structuredSubNode_listOfMapsAreAllExtracted() {
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, Lists.newArrayList(
                ImmutableMap.of(Ontology.DATE_PERIOD_START_DATE, "1920", Ontology.DATE_PERIOD_END_DATE, "1940"),
                ImmutableMap.of(Ontology.DATE_PERIOD_START_DATE, "1941", Ontology.DATE_PERIOD_END_DATE, "1950")));

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(2, extracted.size());
    }

    @Test
    public void structuredSubNode_isRemovedFromInputRegardlessOfShape() {
        // An unrecognised shape (here, a bare String) is logged and skipped rather
        // than thrown - but the DatePeriod key is still consumed either way.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, "not a map or list");

        List<Map<String, Object>> extracted = extractDates(data);

        assertTrue(extracted.isEmpty());
        assertFalse(data.containsKey(Entities.DATE_PERIOD));
    }

    @Test
    public void structuredSubNode_standardDateAttributesAreMovedAndWiden() {
        // Simulates EAD3's unitdatestructured/daterange/fromdate/@standarddate,
        // captured by ead3.properties into these temporary keys.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                START_STANDARD_DATE, "1939-04",
                END_STANDARD_DATE, "1945-06"));

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("1939-04-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1945-06-30", period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(DatePeriod.DatePrecision.month.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
        assertFalse(period.containsKey(START_STANDARD_DATE));
        assertFalse(period.containsKey(END_STANDARD_DATE));
    }

    @Test
    public void structuredSubNode_explicitPrecisionOverridesInference() {
        // Simulates EAD3's @localtype="quarter" - ISO 8601 can't express a quarter,
        // so the month-truncated @standarddate alone would otherwise infer "month".
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                START_STANDARD_DATE, "1939-04",
                END_STANDARD_DATE, "1945-06",
                Ontology.DATE_PERIOD_PRECISION, "quarter"));

        Map<String, Object> period = onlyExtracted(data);

        assertEquals(DatePeriod.DatePrecision.quarter.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
    }

    @Test
    public void structuredSubNode_unparseableDateIsKeptAsIs() {
        // There's no fallback for a structured sub-node (unlike the two text-driven
        // sources below), so a bad value is left untouched rather than dropped.
        Map<String, Object> data = itemDataWith(Entities.DATE_PERIOD, ImmutableMap.of(
                Ontology.DATE_PERIOD_START_DATE, "circa 1939"));

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("circa 1939", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertNull(period.get(Ontology.DATE_PERIOD_PRECISION));
    }

    // --- EAD2002 unitdate/@normal, reconciled against unitDates text ------------

    @Test
    public void normalAttribute_singleDateHasNoEnd() {
        Map<String, Object> data = itemDataWith("unitDates", "1933");
        data.put("unitDatesNormal", "1933");

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("1933-01-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertFalse(period.containsKey(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(DatePeriod.DatePrecision.year.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
        assertEquals(DatePeriod.DatePeriodType.creation.name(), period.get(Ontology.DATE_PERIOD_TYPE));
    }

    @Test
    public void normalAttribute_rangeIsSplitOnSlash() {
        Map<String, Object> data = itemDataWith("unitDates", "1939-1945");
        data.put("unitDatesNormal", "1939-04/1945-06");

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("1939-04-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1945-06-30", period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(DatePeriod.DatePrecision.month.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
        assertEquals("1939-1945", period.get(Ontology.DATE_HAS_DESCRIPTION));
    }

    @Test
    public void normalAttribute_compactFormWithoutHyphensIsAccepted() {
        // The form Ead2002Exporter itself writes: no separators, always 2-digit.
        Map<String, Object> data = itemDataWith("unitDates", "1940-1944");
        data.put("unitDatesNormal", "19400101/19441231");

        Map<String, Object> period = onlyExtracted(data);

        assertEquals("1940-01-01", period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("1944-12-31", period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals(DatePeriod.DatePrecision.day.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
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
        // Two unitdates but only one @normal: it's not safe to say which text the
        // normal value belongs to, so normal-attribute resolution is skipped
        // entirely and both texts go through pattern matching instead - which
        // still recovers "1939-1945" on its own.
        Map<String, Object> data = itemDataWith("unitDates", Lists.newArrayList("1939-1945", "circa 1978"));
        data.put("unitDatesNormal", Lists.newArrayList("1939/1945"));

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(1, extracted.size());
        assertEquals("1939-1945", extracted.get(0).get(Ontology.DATE_HAS_DESCRIPTION));
        assertEquals("circa 1978", data.get("unitDates"));
    }

    @Test
    public void normalAttribute_invalidValueFallsBackAndIsPreservedAsText() {
        // "garbage" isn't a recognisable normal value, and the unitdate text isn't
        // recognisable by pattern-matching either, so nothing is extracted and the
        // original text is left in place rather than lost.
        Map<String, Object> data = itemDataWith("unitDates", "not a real date");
        data.put("unitDatesNormal", "garbage");

        List<Map<String, Object>> extracted = extractDates(data);

        assertTrue(extracted.isEmpty());
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
        // A representative sample of the historical institution-specific formats
        // datePatterns supports; see DateParser for the full list.
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

        List<Map<String, Object>> extracted = extractDates(data);

        assertTrue(extracted.isEmpty());
        assertEquals("not a date at all", data.get("unitDates"));
    }

    @Test
    public void patternMatching_commaSeparatedValuesAreSplitAndMatchedIndependently() {
        // Comma-split segments are matched as-is, without trimming - see below.
        Map<String, Object> data = itemDataWith("unitDates", "1934,1978");

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(2, extracted.size());
        assertFalse(data.containsKey("unitDates"));
    }

    @Test
    public void patternMatching_leadingWhitespaceAfterACommaIsNotTrimmedBeforeMatching() {
        // A space after the comma - easy to introduce from "1934, 1978"-style source
        // data - is enough to make " 1978" fail every pattern, since none of them
        // allow for arbitrary surrounding whitespace on a lone year. It's preserved
        // (trimmed this time) as leftover text rather than silently dropped.
        Map<String, Object> data = itemDataWith("unitDates", "1934, 1978");

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(1, extracted.size());
        assertEquals("1934", extracted.get(0).get(Ontology.DATE_HAS_DESCRIPTION));
        assertEquals("1978", data.get("unitDates"));
    }

    @Test
    public void patternMatching_partiallyMatchedValuesLeaveTheRestAsText() {
        Map<String, Object> data = itemDataWith("unitDates", "1934, not parseable");

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(1, extracted.size());
        assertEquals("not parseable", data.get("unitDates"));
    }

    // --- DatePeriodType: only unitDates is implicitly a creation date -----------

    @Test
    public void datePeriodType_unitDatesIsCreationButOtherPropertiesAreNot() {
        Map<String, Object> data = itemDataWith("unitDates", "1934");
        data.put("existDate", "1900");

        List<Map<String, Object>> extracted = extractDates(data);

        assertEquals(2, extracted.size());
        Map<String, Object> unitDatePeriod = extracted.stream()
                .filter(p -> "1934".equals(p.get(Ontology.DATE_HAS_DESCRIPTION))).findFirst().orElseThrow(AssertionError::new);
        Map<String, Object> existDatePeriod = extracted.stream()
                .filter(p -> "1900".equals(p.get(Ontology.DATE_HAS_DESCRIPTION))).findFirst().orElseThrow(AssertionError::new);

        assertEquals(DatePeriod.DatePeriodType.creation.name(), unitDatePeriod.get(Ontology.DATE_PERIOD_TYPE));
        assertNull(existDatePeriod.get(Ontology.DATE_PERIOD_TYPE));
    }

    // --- normaliseDate(): the widening primitive used by every source above -----

    @Test
    public void normaliseDate_startOfPeriod() {
        assertEquals("1944-01-01", normaliseDate("1944"));
        assertEquals("1944-01-01", normaliseDate("1944-01"));
        assertEquals("1944-06-15", normaliseDate("1944-06-15"));
    }

    @Test
    public void normaliseDate_endOfPeriod() {
        assertEquals("1944-12-31", normaliseDate("1944", true));
        assertEquals("1944-01-31", normaliseDate("1944-01", true));
        // A date that's already fully specified has no period left to widen -
        // including when it's not zero-padded, which previously fooled the
        // lenient "is this actually just a year-month?" check into partially
        // matching the "yyyy-MM" prefix and incorrectly widening it anyway.
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

    private static void assertPeriod(String raw, String start, String end, DatePeriod.DatePrecision precision) {
        Map<String, Object> period = onlyExtracted(itemDataWith("unitDates", raw));
        assertEquals("start date for \"" + raw + "\"", start, period.get(Ontology.DATE_PERIOD_START_DATE));
        assertEquals("end date for \"" + raw + "\"", end, period.get(Ontology.DATE_PERIOD_END_DATE));
        assertEquals("precision for \"" + raw + "\"", precision.name(), period.get(Ontology.DATE_PERIOD_PRECISION));
    }
}
