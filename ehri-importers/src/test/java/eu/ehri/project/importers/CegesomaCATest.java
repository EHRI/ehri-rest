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
 * Test the import of a Cegesoma CA EAD file.
 * This file was based on BundesarchiveTest.java.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Ben Companjen (http://github.com/bencomp)
 */
public class CegesomaCATest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE_NL = "CS-foto-188845-nl.xml";
    protected final String XMLFILE_FR = "CS-foto-188845-fr.xml";
    protected final String ARCHDESC = "CA FE 1437";
    DocumentaryUnit archdesc;
    int origCount=0;
            
    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Cegesoma EAD";

        origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE_NL);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("cegesomaCA.properties")).importFile(ios, logMessage);
        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
       	// - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 2 more subjectAccess nodes
        // - 1 UP
        // - 2 more import Event links (1 for each Unit, 1 for the User)
        // - 1 more import Event
        // --- = 9
        int newCount = origCount + 9;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-ca-fe-1437", archdesc.getId());

        /**
         * Test titles
         */
        // There should be one DocumentDescription for the <archdesc>
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){
            assertTrue(dd.getName().startsWith("Dessin caricatural d'un juif ayant "));
            assertEquals("fra", dd.getLanguageOfDescription());
        }
        
       
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// unitDates still around?
        	String s = d.asVertex().getProperty("unitDates");
        	assertEquals("1940-1945", s);
        	
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(1, dp.size());
        	assertEquals("1940-01-01", dp.get(0).getStartDate());
        	assertEquals("1945-12-31", dp.get(0).getEndDate());
        	
        	
        }
        
    }
}
