package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class MapImporterTest {
    
    public MapImporterTest() {
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
