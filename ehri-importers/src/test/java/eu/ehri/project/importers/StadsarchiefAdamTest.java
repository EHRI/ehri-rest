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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class StadsarchiefAdamTest extends AbstractImporterTest{
    
	private static final Logger logger = LoggerFactory.getLogger(StadsarchiefAdamTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "stadsarchief30602.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String ARCHDESC = "NL-SAA-22626598", //"197a",
            C01 = "NL-SAA-22730932",
            C02 = "NL-SAA-22730310",
            C02_1 = "NL-SAA-22730311",
            C03 = "NL-SAA-22752512",
            C03_2 = "NL-SAA-22752538";
    int origCount=0;

    @Test
    public void niodEadTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a Stadsarchief EAD, with preprocessing done";

        origCount = getNodeCount(graph);
        
 // Before...
//       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        @SuppressWarnings("unused")
		ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("stadsarchief.properties")).importFile(ios, logMessage);
 // After...
//       List<VertexProxy> graphState2 = getGraphState(graph);
//       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);
       

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 6 more DocumentaryUnits (archdesc, 5 children)
       	// - 6 more DocumentDescription
        // - 1 more DatePeriod
        // - 6 more UnknownProperties 
        // - 7 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 3 more MaintenanceEvents
        int newCount = origCount + 30; 
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
        assertEquals("nl-r1-NL-SAA-22626598".toLowerCase(), archdesc.getId());
        assertEquals("nl-r1-NL-SAA-22626598-NL-SAA-22730932".toLowerCase(), c1.getId());
        assertEquals("nl-r1-NL-SAA-22626598-NL-SAA-22730932-NL-SAA-22730310".toLowerCase(), c2.getId());
        assertEquals("nl-r1-NL-SAA-22626598-NL-SAA-22730932-NL-SAA-22730311-NL-SAA-22752512".toLowerCase(), c3.getId());
        assertEquals("nl-r1-NL-SAA-22626598-NL-SAA-22730932-NL-SAA-22730311".toLowerCase(), c2_1.getId());
        assertEquals("nl-r1-NL-SAA-22626598-NL-SAA-22730932-NL-SAA-22730311-NL-SAA-22752538".toLowerCase(), c3_2.getId());

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1.getParent());
        assertEquals(archdesc, c1.getPermissionScope());
        assertEquals(c1, c2.getParent());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2_1, c3.getParent());
        assertEquals(c2_1, c3.getPermissionScope());
        assertEquals(c1, c2_1.getParent());
        assertEquals(c1, c2_1.getPermissionScope());
        assertEquals(c2_1, c3_2.getParent());
        assertEquals(c2_1, c3_2.getPermissionScope());


    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Collectie Bart de Kok en Jozef van Poppel", d.getName());
            for(DatePeriod p : d.getDatePeriods()){
                assertEquals("1931-01-01", p.getStartDate());
            }
            boolean hasScopeAndContent=false;
            boolean hasLanguageOfMaterial=false;
            for(String property : d.asVertex().getPropertyKeys()){
                if(property.equals("scopeAndContent")){
                    hasScopeAndContent=true;
                    assertTrue(d.asVertex().getProperty(property).toString().startsWith("Inleiding"));
                }else if(property.equals("languageOfMaterial")){
                    hasLanguageOfMaterial=true;
                    assertEquals("nld", d.asVertex().getProperty(property).toString());
                }
            }
            assertTrue(hasScopeAndContent);
            assertTrue(hasLanguageOfMaterial);
        }
        for(DocumentDescription desc : c1.getDocumentDescriptions()){
                assertEquals("Documentaire foto's door Bart de Kok", desc.getName());
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
    
        boolean c3HasOtherIdentifier=false;
        for(String property : c3.asVertex().getPropertyKeys()){
            if(property.equals("otherIdentifiers")){
                assertEquals("29", c3.asVertex().getProperty(property).toString());
                c3HasOtherIdentifier=true;
            }
        }
        assertTrue(c3HasOtherIdentifier);
        boolean c3HasRef=false;
        for(DocumentDescription d : c3.getDocumentDescriptions()){
            for(String property : d.asVertex().getPropertyKeys()){
                if(property.equals("ref")){
                    assertEquals("http://beeldbank.amsterdam.nl/beeldbank/weergave/search/layout/result/indeling/grid?f_sk_archief=30602/29",
                        d.asVertex().getProperty(property).toString() );
                    c3HasRef=true;
                }
            }
        }
        assertTrue(c3HasRef);
        
    }
}
    
