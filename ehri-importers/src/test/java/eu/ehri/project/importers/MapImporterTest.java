package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.PermissionScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class MapImporterTest extends AbstractImporterTest{
    
    public MapImporterTest() {
    }
    private Map<String, Object> mapWithOneParseableDate;
    private Map<String, Object> mapWithMultipleDates;
    private Map<String, Object> mapWithMultipleDatesAsList;
    private PermissionScope agent;
    private MapImporter importer;

    @Before
    public void init() throws ItemNotFound{
       agent  = manager.getFrame(TEST_REPO, PermissionScope.class);
       importer = new EadImporter(graph, agent, new ImportLog(null));
       mapWithOneParseableDate = new HashMap<String, Object>();
       mapWithOneParseableDate.put("unitDates", "1934/1936");

       mapWithMultipleDates = new HashMap<String, Object>();
       mapWithMultipleDates.put("unitDates", "1934/1936, summer 1978");
       mapWithMultipleDates.put("existDate", "1900");

           mapWithMultipleDatesAsList = new HashMap<String, Object>();
           List<String> datelist = new ArrayList<String>();
           datelist.add("1934/1936");
           datelist.add("1978");
       mapWithMultipleDatesAsList.put("unitDates", datelist);
       
}

   @Test
   public void splitDatesFromDateProperty(){
       Map<String, String> dates = MapImporter.returnDatesAsString(mapWithOneParseableDate, new XmlImportProperties("dates.properties"));
       assertTrue(dates.containsKey("1934/1936"));
   }
   
    @Test
    public void extractDatesFromDateProperty() throws ItemNotFound {
        List<Map<String, Object>> extractedDates = importer.extractDates(mapWithOneParseableDate);
        
        for(Map<String, Object> dateMap : extractedDates){
            assertEquals("1934/1936", dateMap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
    }
    
    @Test
    public void removeDateFromDateProperty() throws ItemNotFound{
        assertTrue(mapWithOneParseableDate.containsKey("unitDates"));
        importer.replaceDates(mapWithOneParseableDate, importer.extractDates(mapWithOneParseableDate));
        assertFalse(mapWithOneParseableDate.containsKey("unitDates"));
    }
    
    @Test
    public void removeDatesFromDateProperty() throws ItemNotFound{
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        assertTrue(mapWithMultipleDates.containsKey("existDate"));
        importer.replaceDates(mapWithMultipleDates, importer.extractDates(mapWithMultipleDates));
        assertTrue(mapWithMultipleDates.containsKey("unitDates"));
        assertEquals("summer 1978", mapWithMultipleDates.get("unitDates"));
        assertFalse(mapWithMultipleDates.containsKey("existDate"));
    }

    @Test
    public void removeDatesFromDatePropertyList() throws ItemNotFound{
        assertTrue(mapWithMultipleDatesAsList.containsKey("unitDates"));
        importer.replaceDates(mapWithMultipleDatesAsList, importer.extractDates(mapWithMultipleDatesAsList));
        assertFalse(mapWithMultipleDatesAsList.containsKey("unitDates"));
    }
    
    @Test
    public void beginDateYear(){
        assertEquals("1944-01-01", MapImporter.normaliseDate("1944", Ontology.DATE_PERIOD_START_DATE));
    }
    
    @Test
    public void beginDateYearMonth(){
        
        assertEquals("1944-01-01", MapImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_START_DATE));
        
    }

    @Test
    public void endDateYear(){
        assertEquals("1944-12-31", MapImporter.normaliseDate("1944", Ontology.DATE_PERIOD_END_DATE));
    }

    @Test
    public void endDateYearMonth(){
        assertEquals("1944-01-31", MapImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_END_DATE));
        
    }

}
