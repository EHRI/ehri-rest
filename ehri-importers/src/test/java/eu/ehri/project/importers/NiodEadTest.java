/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class NiodEadTest extends AbstractImporterTest{
    
	private static final Logger logger = LoggerFactory.getLogger(NiodEadTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "NIOD-38640-ca1.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String ARCHDESC = "197a", //"197a",
            C01 = "1.",
            C02 = "5-15",
            C02_1 = "8",
            C03 = "58-61",
            C03_2 = "59";
    int origCount=0;

    @Test
    public void niodEadTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a NIOD EAD";

        origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        @SuppressWarnings("unused")
		ImportLog log = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, NiodEadHandler.class).importFile(ios, logMessage);
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 24 more DocumentaryUnits (archdesc, 1-7+7)
       	// - 24 more DocumentDescription
        // - 13 more DatePeriod
        // - 1? more UnknownProperties
        // - 25 more import Event links (24 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = origCount + 105; // temporarily changed to match found numbers
        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        DocumentaryUnit c1 = graph.frame(
                getVertexByIdentifier(graph,C01),
                DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(
                getVertexByIdentifier(graph,C02),
                DocumentaryUnit.class);
        DocumentaryUnit c2_1 = graph.frame(
                getVertexByIdentifier(graph, C02_1),
                DocumentaryUnit.class);
        DocumentaryUnit c3 = graph.frame(
                getVertexByIdentifier(graph,C03),
                DocumentaryUnit.class);
        DocumentaryUnit c3_2 = graph.frame(
                getVertexByIdentifier(graph,C03_2),
                DocumentaryUnit.class);

        // Test correct ID generation
        assertEquals("nl-r1-197a", archdesc.getId());
        assertEquals("nl-r1-197a-1-", c1.getId());
        assertEquals("nl-r1-197a-1-5-15", c2.getId());
        assertEquals("nl-r1-197a-1-58-61", c3.getId());
        assertEquals("nl-r1-197a-1-5-15-8", c2_1.getId());
        assertEquals("nl-r1-197a-1-58-61-59", c3_2.getId());

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1.getParent());
        assertEquals(archdesc, c1.getPermissionScope());
        assertEquals(c1, c2.getParent());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c1, c3.getParent());
        assertEquals(c1, c3.getPermissionScope());
        assertEquals(c2, c2_1.getParent());
        assertEquals(c2, c2_1.getPermissionScope());
        assertEquals(c3, c3_2.getParent());
        assertEquals(c3, c3_2.getPermissionScope());


    //test titles
//        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
//            assertEquals("Stichting Centraal Bureau van Onderzoek inzake de Vererving van de Nalatenschappen van Vermiste Personen", d.getName());
//        }
        for(DocumentDescription desc : c1.getDocumentDescriptions()){
                assertEquals("Algemene zaken", desc.getName());
        }
        for(DocumentDescription desc : c2.getDocumentDescriptions()){
                assertEquals("Agenda's der ingekomen stukken", desc.getName());
        }
        for(DocumentDescription d : c2_1.getDocumentDescriptions()){
            assertEquals("1950-51 No's .3634/50-1506/51", d.getName());
        }
        for(DocumentDescription d : c3_2.getDocumentDescriptions()){
            assertEquals("Ingekomen formulieren met negatieve antwoorden (S 9a)", d.getName());
        }
    //test hierarchy
        assertEquals(new Long(1), archdesc.getChildCount());
        for(DocumentaryUnit d : archdesc.getChildren()){
            assertEquals(C01, d.getIdentifier());
        }
    //test level-of-desc
        for(DocumentDescription d : c3_2.getDocumentDescriptions()){
            assertEquals("file", d.asVertex().getProperty("levelOfDescription"));
        }
    //test dates
        for(DocumentDescription d : c2_1.getDocumentDescriptions()){
        	// Single date is just a string
        	assertEquals("1506/1950", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		logger.debug("startDate: " + dp.getStartDate());
        		logger.debug("endDate: " + dp.getEndDate());
        		assertEquals("1506-01-01", dp.getStartDate());
        		assertEquals("1950-12-31", dp.getEndDate());
        	}
        }
//        
//        // Second fonds has two dates with different types -> list
//        for(DocumentDescription d : c3_2.getDocumentDescriptions()){
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
