package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the import of a memorial de la shoah EAD file.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class MemShoahTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "memshoah.xml";
    protected final String ARCHDESC = "II, V, VI, VIa";
    DocumentaryUnit archdesc;
    int origCount=0;
            
    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example MemShoah EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("memshoah.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);
        
        printGraph(graph);
        // How many new nodes will have been created? We should have
        /** 
         * relationship: 2
         * events: 2
         * documentaryUnit: 1
         * documentDescription: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 1
         */ 

        int newCount = origCount + 9;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        
        for (DocumentDescription d : archdesc.getDocumentDescriptions()) {
            assertEquals("Ambassade d'Allemagne (German Embassy)", d.getName());
            assertEquals("eng", d.getLanguageOfDescription());
        }

    
        //test MaintenanceEvent order
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){

            boolean meFound = false;
            int countME=0;
            for(MaintenanceEvent me : dd.getMaintenanceEvents()){
                meFound=true;
                countME++;
                if(me.asVertex().getProperty("order").equals(0)){
                    assertEquals(MaintenanceEvent.EventType.CREATED.toString(), me.asVertex().getProperty("eventType"));
                }else{
                    assertEquals(MaintenanceEvent.EventType.REVISED.toString(), me.asVertex().getProperty("eventType"));
                }
            }
            assertTrue(meFound);
            assertEquals(1, countME);
        }
        
        
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(1, dp.size());
        	assertEquals("1939-01-01", dp.get(0).getStartDate());
        	assertEquals("1943-12-31", dp.get(0).getEndDate());
        }
        
    }
}
