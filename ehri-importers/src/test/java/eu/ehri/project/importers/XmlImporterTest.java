/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class XmlImporterTest {
    
    public XmlImporterTest() {
    }

   
    
    @Test
    public void beginDateYear(){
        assertEquals("1944-01-01", XmlImporter.normaliseDate("1944", Ontology.DATE_PERIOD_START_DATE));
    }
    
    @Test
    public void beginDateYearMonth(){
        
        assertEquals("1944-01-01", XmlImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_START_DATE));
        
    }

    @Test
    public void endDateYear(){
        assertEquals("1944-12-31", XmlImporter.normaliseDate("1944", Ontology.DATE_PERIOD_END_DATE));
    }

    @Test
    public void endDateYearMonth(){
        assertEquals("1944-01-31", XmlImporter.normaliseDate("1944-01", Ontology.DATE_PERIOD_END_DATE));
        
    }

}
