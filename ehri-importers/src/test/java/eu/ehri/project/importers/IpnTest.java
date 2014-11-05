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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class IpnTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(IpnTest.class);
    protected final String TEST_REPO = "r1";
    protected final String BRANCH_1_XMLFILE = "polishBranch.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String BRANCH_1_ARCHDESC = "Pamięci Narodowej", 
            BRANCH_1_C01_1 = "2746",
            BRANCH_1_C01_2 = "2747";
    int origCount=0;

    protected final String BRANCH_2_XMLFILE = "polishBranch_2.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String BRANCH_2_ARCHDESC = "Biuro Udostępniania", 
            BRANCH_2_C01_1 = "1",
            BRANCH_2_C01_2 = "2";

    protected final String VC_XMLFILE = "IpnVirtualCollection.xml";

    @Test
    public void polishVirtualCollectionTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the IPN Virtual Collection";

        InputStream ios1 = ClassLoader.getSystemResourceAsStream(BRANCH_1_XMLFILE);
	new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("polishBranch.properties")).importFile(ios1, logMessage);
        
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(BRANCH_2_XMLFILE);
	new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("polishBranch.properties")).importFile(ios2, logMessage);
        
        origCount = getNodeCount(graph);
        InputStream iosVc = ClassLoader.getSystemResourceAsStream(VC_XMLFILE);
	new SaxImportManager(graph, agent, validUser, VirtualEadImporter.class, VirtualEadHandler.class, new XmlImportProperties("vc.properties")).importFile(iosVc, logMessage);
        
        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 3 more VirtualUnits (archdesc, 2 children with each 2 children)
       	// - 3 more DocumentDescription
        // - 4 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event

        // - 0 more MaintenanceEvents
        int newCount = origCount + 11; 
        assertEquals(newCount, getNodeCount(graph));
        
        VirtualUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,"ipn vc"),
                VirtualUnit.class);
        
        assertEquals(2L, archdesc.getChildCount());
        

    }
    
    @Test
    public void polishBranch_1_EadTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a the IPN Polish Branches EAD, without preprocessing done";

        origCount = getNodeCount(graph);
        
 // Before...
//       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(BRANCH_1_XMLFILE);
        @SuppressWarnings("unused")
	ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("polishBranch.properties")).importFile(ios, logMessage);
 // After...
//       List<VertexProxy> graphState2 = getGraphState(graph);
//       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);
       

//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 3 more DocumentaryUnits (archdesc, 2 children)
       	// - 3 more DocumentDescription
        // - 3 more UnknownProperties 
        // - 4 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 more DatePeriod

        // - 0 more MaintenanceEvents
        int newCount = origCount + 15; 
        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,BRANCH_1_ARCHDESC),
                DocumentaryUnit.class);
        DocumentaryUnit c1_1 = graph.frame(
                getVertexByIdentifier(graph,BRANCH_1_C01_1),
                DocumentaryUnit.class);
        DocumentaryUnit c1_2 = graph.frame(
                getVertexByIdentifier(graph,BRANCH_1_C01_2),
                DocumentaryUnit.class);

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1_1.getParent());
        assertEquals(archdesc, c1_1.getPermissionScope());
        assertEquals(archdesc, c1_2.getParent());
        assertEquals(archdesc, c1_2.getPermissionScope());


    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Collections from Oddział Instytutu Pamięci Narodowej we Wrocławiu", d.getName());
            boolean hasProvenance=false;
            for(String property : d.asVertex().getPropertyKeys()){
                if(property.equals("processInfo")){
                    hasProvenance=true;
                    System.out.println(d.asVertex().getProperty(property));
                    assertTrue(d.asVertex().getProperty(property).toString().startsWith("This selection has been "));
                }
            }
            assertTrue(hasProvenance);
        }
        for(DocumentDescription desc : c1_1.getDocumentDescriptions()){
                assertEquals("Cukrownia w Pszennie – August Gross i Synowie [August Gross & Söhne Zuckerfabrik Weizenrodau]", desc.getName());
        }
    //test hierarchy
        assertEquals(2L, archdesc.getChildCount());
        
    //test level-of-desc
        for(DocumentDescription d : c1_1.getDocumentDescriptions()){
            assertEquals("collection", d.asVertex().getProperty("levelOfDescription"));
        }
    // test dates
        boolean hasDates = false;
        for(DocumentDescription d : c1_1.getDocumentDescriptions()){
            for(DatePeriod p : d.getDatePeriods()){
                hasDates=true;
            }
        }
        assertTrue(hasDates);
        
    }

    @Test
    public void polishBranch_2_EadTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a the IPN Polish Branches EAD, without preprocessing done";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(BRANCH_2_XMLFILE);
        @SuppressWarnings("unused")
	ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("polishBranch.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
       

//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 3 more DocumentaryUnits (archdesc, 2 children)
       	// - 3 more DocumentDescription
        // - 3 more UnknownProperties 
        // - 4 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 more DatePeriod

        // - 0 more MaintenanceEvents
        int newCount = origCount + 15; 
        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph,BRANCH_2_ARCHDESC),
                DocumentaryUnit.class);
        DocumentaryUnit c1_1 = graph.frame(
                getVertexByIdentifier(graph,BRANCH_2_C01_1),
                DocumentaryUnit.class);
        DocumentaryUnit c1_2 = graph.frame(
                getVertexByIdentifier(graph,BRANCH_2_C01_2),
                DocumentaryUnit.class);

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1_1.getParent());
        assertEquals(archdesc, c1_1.getPermissionScope());
        assertEquals(archdesc, c1_2.getParent());
        assertEquals(archdesc, c1_2.getPermissionScope());


    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Collections from Biuro Udostępniania i Archiwizacji Dokumentów w Warszawie", d.getName());
            boolean hasProvenance=false;
            for(String property : d.asVertex().getPropertyKeys()){
                if(property.equals("processInfo")){
                    hasProvenance=true;
                    System.out.println(d.asVertex().getProperty(property));
                    assertTrue(d.asVertex().getProperty(property).toString().startsWith("This selection has been "));
                }
            }
            assertTrue(hasProvenance);
        }
        for(DocumentDescription desc : c1_1.getDocumentDescriptions()){
                assertEquals("Areszt Śledczy Sądowy w Poznaniu [Untersuchungshaftanstalt Posen]", desc.getName());
        }
    //test hierarchy
        assertEquals(2L, archdesc.getChildCount());
        
    //test level-of-desc
        for(DocumentDescription d : c1_1.getDocumentDescriptions()){
            assertEquals("collection", d.asVertex().getProperty("levelOfDescription"));
        }
    // test dates
        boolean hasDates = false;
        for(DocumentDescription d : c1_1.getDocumentDescriptions()){
            for(DatePeriod p : d.getDatePeriods()){
                hasDates=true;
                assertEquals("1940-01-01", p.getStartDate());
                assertEquals("1943-12-31", p.getEndDate());
            }
        }
        assertTrue(hasDates);
        
    }

}
    
