/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author linda
 */
public class BundesarchiveSplitTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "BA_split.xml";
    protected final String ARCHDESC = "NS 1";
    int origCount=0;
            
    @Test
    public void bundesarchiveTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the Split Bundesarchive EAD";

        origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("bundesarchive.properties")).importFile(ios, logMessage);
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
       	// - 1 more DocumentDescription
        // - 2 more DatePeriod
        // - 1 more UnknownProperties
        // - 3 more Relationships
        // - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = origCount + 9+2;

        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);

        // Test ID generation and hierarchy
        assertEquals("nl-r1-ns-1", archdesc.getId());

        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());


    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Reichsschatzmeister der NSDAP", d.getName());
        }
    //test dates
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// Single date is just a string
        	assertEquals("1906/19, 1922/45", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1906-01-01", dp.getStartDate());
        		assertEquals("1919-12-31", dp.getEndDate());
                        break;
        	}
        }
        
//        // Second fonds has two dates with different types -> list
//        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
//        	// unitDates still around?
//        	assertEquals("1943-1944", d.asVertex().getProperty("unitDates"));
//        	// start and end dates correctly parsed and setup
//        	for(DatePeriod dp : d.getDatePeriods()){
//        		assertEquals("1943-01-01", dp.getStartDate());
//        		assertEquals("1944-12-31", dp.getEndDate());
//        	}
//        	
//        	// Since there was a list of unitDateTypes, it should now be deleted
//        	assertNull(d.asVertex().getProperty("unitDatesTypes"));
//        }
        
    }
}
