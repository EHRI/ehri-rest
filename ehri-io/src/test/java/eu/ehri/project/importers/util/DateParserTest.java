package eu.ehri.project.importers.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.importers.util.DateParser.normaliseDate;
import static org.junit.Assert.*;

public class DateParserTest {
    private Map<String, Object> mapWithOneParseableDate;
    private Map<String, Object> mapWithMultipleDates;
    private Map<String, Object> mapWithMultipleDatesAsList;

    @Before
    public void init() throws ItemNotFound {
        mapWithOneParseableDate = Maps.newHashMap();
        mapWithOneParseableDate.put("unitDates", "1934/1936");

        mapWithMultipleDates = Maps.newHashMap();
        mapWithMultipleDates.put("unitDates", "1934/1936, summer 1978");
        mapWithMultipleDates.put("existDate", "1900");

        mapWithMultipleDatesAsList = Maps.newHashMap();
        List<String> datelist = Lists.newArrayList();
        datelist.add("1934/1936");
        datelist.add("1978");
        mapWithMultipleDatesAsList.put("unitDates", datelist);
    }

    @Test
    public void extractDatesFromDateProperty() throws ItemNotFound {
        List<Map<String, Object>> extractedDates = ImportHelpers
                .extractDates(mapWithOneParseableDate);
        for (Map<String, Object> dateMap : extractedDates) {
            assertEquals("1934/1936", dateMap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
    }

    @Test
    public void removeDateFromDateProperty() throws ItemNotFound {
        assertTrue(mapWithOneParseableDate.containsKey("unitDates"));
        ImportHelpers.extractDates(mapWithOneParseableDate);
        assertFalse(mapWithOneParseableDate.containsKey("unitDates"));
    }

    @Test
    public void removeDatesFromDateProperty() throws ItemNotFound {
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        assertTrue(mapWithMultipleDates.containsKey("existDate"));
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        ImportHelpers.extractDates(mapWithMultipleDates);
        assertEquals("summer 1978", mapWithMultipleDates.get("unitDates"));
        assertFalse(mapWithMultipleDates.containsKey("existDate"));
    }

    @Test
    public void removeDatesFromDatePropertyList() throws ItemNotFound {
        assertTrue(mapWithMultipleDatesAsList.containsKey("unitDates"));
        ImportHelpers.extractDates(mapWithMultipleDatesAsList);
        assertFalse(mapWithMultipleDatesAsList.containsKey("unitDates"));
    }

    @Test
    public void beginDateYear() {
        assertEquals("1944-01-01", normaliseDate("1944"));
    }

    @Test
    public void beginDateYearMonth() {
        assertEquals("1944-01-01", normaliseDate("1944-01"));
    }

    @Test
    public void endDateYear() {
        assertEquals("1944-12-31", normaliseDate("1944", true));
    }

    @Test
    public void endDateYearMonth() {
        assertEquals("1944-01-31", normaliseDate("1944-01", true));
    }
}