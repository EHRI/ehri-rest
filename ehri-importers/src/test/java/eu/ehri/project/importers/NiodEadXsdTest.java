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
public class NiodEadXsdTest extends AbstractImporterTest{
    
	private static final Logger logger = LoggerFactory.getLogger(NiodEadXsdTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "NIOD-38474.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String ARCHDESC = "MF1081500", //"197a",
            C01 = "MF1148873",
            C02 = "MF1086379",
            C02_1 = "MF1086380",
            C03 = "MF1086399",
            C03_2 = "MF1086398";
    int origCount=0;

    @Test
    public void niodEadTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a NIOD EAD";

        origCount = getNodeCount(graph);
        
 // Before...
//       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        @SuppressWarnings("unused")
		ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("niodead.properties")).importFile(ios, logMessage);
        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 6 more DocumentaryUnits (archdesc, 5 children)
       	// - 6 more DocumentDescription
        // - 5 more DatePeriod
        // - 2 more UnknownProperties (1 for daogrp)
        // - 7 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = origCount + 27; 
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
        assertEquals("nl-r1-MF1081500".toLowerCase(), archdesc.getId());
        assertEquals("nl-r1-MF1081500-MF1148873".toLowerCase(), c1.getId());
        assertEquals("nl-r1-MF1081500-MF1148873-MF1086379".toLowerCase(), c2.getId());
        assertEquals("nl-r1-MF1081500-MF1148873-MF1086380-MF1086399".toLowerCase(), c3.getId());
        assertEquals("nl-r1-MF1081500-MF1148873-MF1086380".toLowerCase(), c2_1.getId());
        assertEquals("nl-r1-MF1081500-MF1148873-MF1086380-MF1086398".toLowerCase(), c3_2.getId());

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

        //test ref
        boolean hasRef = false;
        for(DocumentDescription d : c1.getDocumentDescriptions()){
          for(String key: d.asVertex().getPropertyKeys()){
            if(key.equals("ref")){
                assertEquals("http://www.archieven.nl/nl/search-modonly?mivast=298&mizig=210&miadt=298&miaet=1&micode=809&minr=1086379&miview=inv2", d.asVertex().getProperty(key));
                hasRef=true;
            }
            logger.debug(key);
          }
        }
        assertTrue(hasRef);

    //test titles
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
            assertEquals("Caransa, A.", d.getName());
        }
        for(DocumentDescription desc : c1.getDocumentDescriptions()){
                assertEquals("Manuscripten, lezingen en onderzoeksmateriaal", desc.getName());
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
        	assertEquals("1980", d.asVertex().getProperty("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		logger.debug("startDate: " + dp.getStartDate());
        		logger.debug("endDate: " + dp.getEndDate());
        		assertEquals("1980-01-01", dp.getStartDate());
        		assertEquals("1980-12-31", dp.getEndDate());
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
    
