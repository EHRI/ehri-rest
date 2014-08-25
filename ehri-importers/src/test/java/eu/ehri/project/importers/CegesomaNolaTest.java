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
 * Test the import of a Cegesoma EAD file.
 * This file was based on BundesarchiveTest.java.
 * @author linda
 * @author ben
 */
public class CegesomaNolaTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "Bestand_Nola_ead-test.pxml";
    protected final String ARCHDESC = "AA 1134",
            C01 = "1234",
            C02_01 = "AA 1134 / 32",
            C02_02 = "AA 1134 / 34";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;
    int origCount=0;
            
    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Cegesoma EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("cegesomaNola.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
        
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        /** 
         * event links: 6
         * relationship: 34
         * documentaryUnit: 5
         * documentDescription: 5
         * unknown property: 1
         * systemEvent: 1
         * datePeriod: 4
         */
        // --- = 56
        int newCount = origCount + 56;
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
        assertEquals("nl-r1-aa-1134-1234", c1.getId());
        assertEquals(c1.getId() + "-aa-1134-32", c2_1.getId());
        assertEquals(c1.getId() + "-aa-1134-34", c2_2.getId());

        /**
         * Test titles
         */
        // There should be one DocumentDescription for the <archdesc>
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){
            assertEquals("Deelarchief betreffende het actienetwerk Nola (1942-1944)", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
            for(String key : dd.asVertex().getPropertyKeys())
                System.out.println(key);
            assertEquals("Automatisch gegenereerd door PALLAS systeem", dd.asVertex().getProperty(EadHandler.AUTHOR));
        }
        
        // There should be one DocumentDescription for the (only) <c01>
        for(DocumentDescription dd : c1.getDocumentDescriptions()){
            assertEquals("Documenten betreffende l'Union nationale de la Résistance", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
        }

        // There should be one DocumentDescription for the (second) <c02>
        for(DocumentDescription dd : c2_2.getDocumentDescriptions()){
            assertEquals("Wetteksten (U.) S.R.A.", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
            assertEquals("item", dd.asVertex().getProperty("levelOfDescription"));
        }
    
        /**
         * Test hierarchy
         */
        assertEquals(new Long(1), archdesc.getChildCount());
        for(DocumentaryUnit du : archdesc.getChildren()){
            assertEquals(C01, du.getIdentifier());
        }
//    //test dates
        for(DocumentDescription d : c2_1.getDocumentDescriptions()){
        	// Single date is just a string
        	assertEquals("1944-1948", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1944-01-01", dp.getStartDate());
        		assertEquals("1948-12-31", dp.getEndDate());
        	}
        }
        
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(2, dp.size());
        	assertEquals("1944-01-01", dp.get(0).getStartDate());
        	assertEquals("1979-12-31", dp.get(0).getEndDate());
        }
        
    }
}
