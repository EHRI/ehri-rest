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

/**
 * Test the import of a Ifz EAD file.
 * This file was based on BundesarchiveTest.java.
 * @author linda
 * @author ben
 */
public class IfzTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "ifz_ED.xml";
    protected final String ARCHDESC = "ED",
            C01 = "ED 457",
            C02_01 = "ED 457 / 185",
            C02_02 = "ED 457 / 186";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;
    int origCount=0;
            
    @Test
    public void ifzTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Ifz EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("ifz.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
        
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        /** 
         * event links: 5
         * relationship: 19
         * documentaryUnit: 4
         * documentDescription: 4
         * unknown property: 2
         * systemEvent: 1
         * maintenanceEvent: 1
         * datePeriod: 2
         */
        int newCount = origCount + 38;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        c1 = graph.frame(
                getVertexByIdentifier(graph,C01),
                DocumentaryUnit.class);
        c2_1 = graph.frame(
                getVertexByIdentifier(graph,C02_01),
                DocumentaryUnit.class);
        c2_2 = graph.frame(
                getVertexByIdentifier(graph,C02_02),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-ed-ed-457", c1.getId());
        assertEquals(c1.getId() + "-ed-457-185", c2_1.getId());
        assertEquals(c1.getId() + "-ed-457-186", c2_2.getId());

        /**
         * Test titles
         */
        // There should be one DocumentDescription for the <archdesc>
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){
            assertEquals("TO BE FILLED", dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            assertEquals("IfZ", dd.asVertex().getProperty("processInfo"));
            assertEquals("recordgrp", dd.asVertex().getProperty("levelOfDescription"));
            
        }
        
        // There should be one DocumentDescription for the (only) <c01>
        for(DocumentDescription dd : c1.getDocumentDescriptions()){
            assertEquals("Lohmeier, Cornelia", dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            assertEquals("series", dd.asVertex().getProperty("levelOfDescription"));
        }

        // There should be one DocumentDescription for the (second) <c02>
        for(DocumentDescription dd : c2_2.getDocumentDescriptions()){
            assertEquals(C02_02, dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            assertEquals("file", dd.asVertex().getProperty("levelOfDescription"));
            assertEquals("www.ifz-muenchen.de/archiv/ED_0066_0001_0000.pdf", dd.asVertex().getProperty("ref"));
        }
    //Band
        for(DocumentDescription dd : c2_1.getDocumentDescriptions()){
            assertEquals("Dachau, III", dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            assertEquals("file", dd.asVertex().getProperty("levelOfDescription"));
            assertEquals("Band", dd.asVertex().getProperty("extentAndMedium"));
        }
        /**
         * Test hierarchy
         */
        assertEquals(1L, archdesc.getChildCount());
        for(DocumentaryUnit du : archdesc.getChildren()){
            assertEquals(C01, du.getIdentifier());
        }
//    //test dates
        for(DocumentDescription d : c2_1.getDocumentDescriptions()){
        	// Single date is just a string
        	assertEquals("1991/2008", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1991-01-01", dp.getStartDate());
        		assertEquals("2008-12-31", dp.getEndDate());
        	}
        }
        
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(0, dp.size());
        }
        
    }
}
