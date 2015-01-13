package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class BundesarchiveVcTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "BA_split.xml";
    protected final String VCFILE = "BA_vc.xml";
    protected final String ARCHDESC = "NS 1";
    int origCount=0;
            
    @Test
    public void bundesarchiveTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the Split Bundesarchive EAD";

        origCount = getNodeCount(graph);
        
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("bundesarchive.properties")).importFile(ios, logMessage);
//        printGraph(graph);
        
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
       	// - 1 more DocumentDescription
        // - 2 more DatePeriod
        // - 1 more UnknownProperties
        // - 3 more Relationships
        // - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 5 more MaintenanceEvents (4 revised, 1 created)
        int newCount = origCount + 9+2+4+1;

        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);

        InputStream iosvc = ClassLoader.getSystemResourceAsStream(VCFILE);
        ImportLog logvc = new SaxImportManager(graph, agent, validUser, VirtualEadImporter.class, VirtualEadHandler.class, new XmlImportProperties("vc.properties")).importFile(iosvc, logMessage);
//        printGraph(graph);
        
        VirtualUnit ss= graph.frame(getVertexByIdentifier(graph, "0.0.0.0"), VirtualUnit.class);
        System.out.println("-----------");
        int countDocumentaryChild=0;
        for(DocumentaryUnit u : ss.getIncludedUnits()){
//            System.out.println(u.getIdentifier());
            countDocumentaryChild++;
        }
        assertEquals(1, countDocumentaryChild);
    }
}
