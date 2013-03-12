/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.test.AbstractImporterTest;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.AgentDescription;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class Eag2896Test extends AbstractImporterTest {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Eag2896Test.class);
     protected final String SINGLE_EAC = "eag-2896.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String IMPORTED_ITEM_ID = "NL-2896";
    protected final String AGENT_DESC = "NL-2896-desc";

    @Test
    public void testImportItemsT()  {
        try {
            Agent agent = manager.getFrame(TEST_REPO, Agent.class); 
           final String logMessage = "Importing a single EAG";

           int count = getNodeCount(graph);
           logger.info("count of nodes before importing: " + count);

           InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
           ImportLog log = new SaxImportManager(graph, agent, validUser, EagImporter.class, EagHandler.class).importFile(ios, logMessage);
           printGraph(graph);
               // How many new nodes will have been created? We should have
               // - 1 more Authority
               // - 1 more AuthorityDescription
               // - 1 more Address
               // - 2 more MaintenanceEvent
               // - 2 more linkEvents (1 for the Authority, 1 for the User)
               // - 1 more SystemEvent        
               assertEquals(count + 8, getNodeCount(graph));

               Iterable<Vertex> docs = graph.getVertices(AccessibleEntity.IDENTIFIER_KEY,
                       IMPORTED_ITEM_ID);
               assertTrue(docs.iterator().hasNext());
               Agent unit = graph.frame(
                       getVertexByIdentifier(graph,IMPORTED_ITEM_ID),
                       Agent.class);
               assertEquals(Entities.AGENT, unit.asVertex().getProperty("__ISA__"));

               // check the child items
               AgentDescription c1 = graph.frame(
                       getVertexByIdentifier(graph,AGENT_DESC),
                       AgentDescription.class);
               assertEquals(Entities.AGENT_DESCRIPTION, c1.asVertex().getProperty("__ISA__"));

               //check whether the description has an Address attached to it
               assertEquals(1, toList(c1.getAddresses()).size());

               assertEquals(2, toList(c1.getMaintenanceEvents()).size());
               // Ensure that c1 is a description of the unit
               for (Description d : unit.getDescriptions()) {
                   assertEquals(IMPORTED_ITEM_ID , d.getDescribedEntity().getIdentifier());
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
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            fail();
        } catch (ValidationError ex) {
            logger.error(ex.getMessage());
            fail();
        } catch (InputParseError ex) {
            logger.error(ex.getMessage());
            fail();
        } catch (ItemNotFound ex) {
            logger.error(ex.getMessage());
            fail();
        }

    }

}
