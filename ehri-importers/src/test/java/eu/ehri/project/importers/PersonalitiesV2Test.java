package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class PersonalitiesV2Test extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesV2Test.class);

    @Test
    public void testAbwehrWithAllReferredNodes() throws Exception {
        final String SINGLE_EAC = "PersonalitiesV2.xml";
        final String logMessage = "Importing EAC " + SINGLE_EAC;
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
        int count = getNodeCount(graph);

        // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        ImportLog log = new SaxImportManager(graph, SystemScope.getInstance(), validUser, EacImporter.class,
                EacHandler.class, new XmlImportProperties("personalitiesv2.properties")).setTolerant(Boolean.TRUE).importFile(ios, logMessage);
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
       /**
        * null: 2
        * relationship: 1
        * historicalAgent: 1
        * maintenanceEvent: 1
        * systemEvent: 1
        * historicalAgentDescription: 1
        */
        assertEquals(count+7, getNodeCount(graph));
        printGraph(graph);
        HistoricalAgent person = manager.getFrame("ehri-pers-000051", HistoricalAgent.class);
        for(Description d : person.getDescriptions()){
            assertEquals("deu", d.getLanguageOfDescription());
            assertTrue(d.asVertex().getProperty("otherFormsOfName") instanceof List);
            assertEquals(2, ((List)d.asVertex().getProperty("otherFormsOfName")).size());
            assertTrue(d.asVertex().getProperty("place") instanceof List);
            assertEquals(2, ((List)d.asVertex().getProperty("place")).size());
        }
    }
}
