package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class EacImporterTest extends AbstractImporterTest {
private static final Logger logger = LoggerFactory.getLogger(EacImporterTest.class);
    protected final String SINGLE_EAC = "algemeyner-yidisher-arbeter-bund-in-lite-polyn-un-rusland.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String IMPORTED_ITEM_ID = "159#object";
    protected final String AUTHORITY_DESC = "159";

    @Test
    public void testImportItemsT() throws Exception{

        final String logMessage = "Importing a single EAC";

        int count = getNodeCount(graph);
        logger.debug("count of nodes before importing: " + count);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
        ImportLog log = new SaxImportManager(graph, SystemScope.getInstance(), validUser, EacImporter.class,
                EacHandler.class).setTolerant(Boolean.TRUE).importFile(ios, logMessage);
        printGraph(graph);
            // How many new nodes will have been created? We should have
            // - 1 more HistoricalAgent
            // - 1 more HistoricalAgentDescription
            // - 2 more MaintenanceEvent 
            // - 2 more linkEvents (1 for the HistoricalAgent, 1 for the User)
            // - 1 more SystemEvent
            assertEquals(count + 7, getNodeCount(graph));

            Iterable<Vertex> docs = graph.getVertices(AccessibleEntity.IDENTIFIER_KEY,
                    IMPORTED_ITEM_ID);
            assertTrue(docs.iterator().hasNext());
            HistoricalAgent unit = graph.frame(
                    getVertexByIdentifier(graph,IMPORTED_ITEM_ID),
                    HistoricalAgent.class);

            // check the child items
            HistoricalAgentDescription c1 = graph.frame(
                    getVertexByIdentifier(graph,AUTHORITY_DESC),
                    HistoricalAgentDescription.class);
            assertEquals(Entities.HISTORICAL_AGENT_DESCRIPTION, c1.asVertex().getProperty("__ISA__"));

            List<String> l = new ArrayList<String>();
            for(String al : (String[])c1.asVertex().getProperty("otherFormsOfName")){
                l.add(al);
            }
            assertTrue(c1.asVertex().getProperty(Description.NAME) instanceof String);

            assertEquals(2, ((String[])c1.asVertex().getProperty("otherFormsOfName")).length);
            // Ensure that c1 is a description of the unit
            for (Description d : unit.getDescriptions()) {
            
            assertEquals(d.getName(), c1.getName());
                assertEquals(d.getEntity().getIdentifier(), unit.getIdentifier());
            }

//TODO: find out why the unit and the action are not connected ...
//            Iterable<Action> actions = unit.getHistory();
//            assertEquals(1, toList(actions).size());
            // Check we've only got one action
            assertEquals(1, log.getCreated());
            assertTrue(log.getAction() instanceof SystemEvent);
            assertEquals(logMessage, log.getAction().getLogMessage());

            // Ensure the import action has the right number of subjects.
            List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
            assertEquals(1, subjects.size());
            assertEquals(log.getSuccessful(), subjects.size());


//        System.out.println("created: " + log.getCreated());

    }


}
