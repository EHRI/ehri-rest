/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author linda
 */
public class BundesarchiveTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "best20130618-testpart.xml";
    protected final String ARCHDESC = "bundesarchiveID7",
            C01 = "bundesarchiveID6",
            C02 = "bundesarchiveID5",
            C07_1 = "RS 2",
            C07_2 = "RS 2-1";
    DocumentaryUnit archdesc, c1, c2, c7_1, c7_2;
    int origCount=0;
            
    @Test
    public void bundesarchiveTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the Bundesarchive EAD";

        origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, BundesarchiveEadImporter.class, BundesarchiveEadHandler.class).importFile(ios, logMessage);
        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 9 more DocumentaryUnits (archdesc, 1-7+7)
       	// - 9 more DocumentDescription
        // - 2 more DatePeriod
        // - 1 more UnknownProperties
        // - 10 more import Event links (9 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = origCount + 32;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        c1 = graph.frame(
                getVertexByIdentifier(graph,C01),
                DocumentaryUnit.class);
        c2 = graph.frame(
                getVertexByIdentifier(graph,C02),
                DocumentaryUnit.class);
        c7_1 = graph.frame(
                getVertexByIdentifier(graph,C07_1),
                DocumentaryUnit.class);
        c7_2 = graph.frame(
                getVertexByIdentifier(graph,C07_2),
                DocumentaryUnit.class);

    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Bestände", d.getName());
        }
        for(DocumentDescription desc : c1.getDocumentDescriptions()){
                assertEquals("Bundesarchiv", desc.getName());
        }
        for(DocumentDescription d : c7_1.getDocumentDescriptions()){
            assertEquals("Generalkommandos der Waffen-SS", d.getName());
        }
        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
            assertEquals("I. SS-Panzerkorps\"Leibstandarte\"", d.getName());
        }
    //test hierarchy
        assertEquals(new Long(1), archdesc.getChildCount());
        for(DocumentaryUnit d : archdesc.getChildren()){
            assertEquals(C01, d.getIdentifier());
        }
    //test arta-identifiers
        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
            assertEquals("R 3021", d.asVertex().getProperty("arta"));
        }
    //test level-of-desc
        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
            assertEquals("fonds", d.asVertex().getProperty("levelOfDescription"));
        }
    //test dates
        for(DocumentDescription d : c7_1.getDocumentDescriptions()){
        	// Single date is just a string
        	assertEquals("1942-1945", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1942-01-01", dp.getStartDate());
        		assertEquals("1945-01-01", dp.getEndDate());
        	}
        	// There was only one date+date type, so it must still be around. 
        	assertEquals("Bestandslaufzeit", d.asVertex().getProperty("unitDatesTypes"));
        }
        
        // Second fonds has two dates with different types -> list
        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
        	// unitDates still around?
        	assertEquals("1943-1944", d.asVertex().getProperty("unitDates"));
        	// start and end dates correctly parsed and setup
        	for(DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1943-01-01", dp.getStartDate());
        		assertEquals("1944-01-01", dp.getEndDate());
        	}
        	
        	// Since there was a list of unitDateTypes, it should now be deleted
        	assertNull(d.asVertex().getProperty("unitDatesTypes"));
        }
        
    }
}
