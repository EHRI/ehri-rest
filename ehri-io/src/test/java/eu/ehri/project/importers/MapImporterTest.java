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

package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.PermissionScope;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MapImporterTest extends AbstractImporterTest {

    public MapImporterTest() {
    }

    private Map<String, Object> mapWithOneParseableDate;
    private Map<String, Object> mapWithMultipleDates;
    private Map<String, Object> mapWithMultipleDatesAsList;
    private MapImporter importer;

    @Before
    public void init() throws ItemNotFound {
        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        importer = new EadImporter(graph, agent, new ImportLog(null));
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
    public void splitDatesFromDateProperty() {
        Map<String, String> dates = MapImporter.returnDatesAsString(mapWithOneParseableDate, new XmlImportProperties("dates.properties"));
        assertTrue(dates.containsKey("1934/1936"));
    }

    @Test
    public void extractDatesFromDateProperty() throws ItemNotFound {
        List<Map<String, Object>> extractedDates = importer.extractDates(mapWithOneParseableDate);

        for (Map<String, Object> dateMap : extractedDates) {
            assertEquals("1934/1936", dateMap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
    }

    @Test
    public void removeDateFromDateProperty() throws ItemNotFound {
        assertTrue(mapWithOneParseableDate.containsKey("unitDates"));
        importer.replaceDates(mapWithOneParseableDate, importer.extractDates(mapWithOneParseableDate));
        assertFalse(mapWithOneParseableDate.containsKey("unitDates"));
    }

    @Test
    public void removeDatesFromDateProperty() throws ItemNotFound {
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        assertTrue(mapWithMultipleDates.containsKey("existDate"));
        importer.replaceDates(mapWithMultipleDates, importer.extractDates(mapWithMultipleDates));
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        assertEquals("summer 1978", mapWithMultipleDates.get("unitDates"));
        assertFalse(mapWithMultipleDates.containsKey("existDate"));
    }

    @Test
    public void removeDatesFromDatePropertyList() throws ItemNotFound {
        assertTrue(mapWithMultipleDatesAsList.containsKey("unitDates"));
        importer.replaceDates(mapWithMultipleDatesAsList, importer.extractDates(mapWithMultipleDatesAsList));
        assertFalse(mapWithMultipleDatesAsList.containsKey("unitDates"));
    }

    @Test
    public void beginDateYear() {
        assertEquals("1944-01-01", MapImporter.normaliseDate("1944", Ontology.DATE_PERIOD_START_DATE));
    }

    @Test
    public void beginDateYearMonth() {

        assertEquals("1944-01-01", MapImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_START_DATE));

    }

    @Test
    public void endDateYear() {
        assertEquals("1944-12-31", MapImporter.normaliseDate("1944", Ontology.DATE_PERIOD_END_DATE));
    }

    @Test
    public void endDateYearMonth() {
        assertEquals("1944-01-31", MapImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_END_DATE));

    }

}
