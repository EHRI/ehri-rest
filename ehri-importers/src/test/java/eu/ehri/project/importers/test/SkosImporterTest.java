/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.SkosHandler;
import eu.ehri.project.importers.SkosImporter;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;

/**
 *
 * @author linda
 */
public class SkosImporterTest extends AbstractImporterTest {

    protected final String SINGLE_SKOS = "skos-agriculture.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    protected final String IMPORTED_ITEM_ID = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=932";
    protected final String IMPORTED_ITEM_DESC_EN = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=932#description_en";
    protected final String IMPORTED_ITEM_DESC_DE = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=932#description_de";

    @Ignore("not ready yet") @Test
    public void testImportItemsT() throws Exception {
        UserProfile user = validUser; //graph.frame(graph.getVertex(validUserId), UserProfile.class);
        Agent agent = manager.getFrame(TEST_REPO, Agent.class); //graph.frame(helper.getTestVertex(TEST_REPO), Agent.class);

        final String logMessage = "Importing a single skos";

        int count = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_SKOS);
        ImportLog log = new SaxImportManager(graph, agent, user, SkosImporter.class, SkosHandler.class).importFile(ios, logMessage);
        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 1 more Concept
        // - 2 more ConceptDescription
        // - 1 more import Action        
        assertEquals(count + 4, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(AccessibleEntity.IDENTIFIER_KEY,
                IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());

        Concept unit = graph.frame(getVertexByIdentifier(graph, IMPORTED_ITEM_ID), Concept.class);

        // check the child items
        ConceptDescription c1 = graph.frame(getVertexByIdentifier(graph, IMPORTED_ITEM_DESC_EN), ConceptDescription.class);
        ConceptDescription c2 = graph.frame(getVertexByIdentifier(graph, IMPORTED_ITEM_DESC_DE), ConceptDescription.class);
        
        // Ensure that c1 is a description of the unit
        assertEquals(c1.getEntity().asVertex().getId(), unit.asVertex().getId());
//            assertEquals(c1.getEntity(), unit); //TODO why does not this work?
        assertEquals(c2.getEntity().asVertex().getId(), unit.asVertex().getId());

//        for (Description d : unit.getDescriptions()) {
//                for(String key: d.getEntity().asVertex().getPropertyKeys()){
//                    System.out.println(key + " " + d.getEntity().asVertex().getProperty(key));
//                }
//                for(Edge e: d.getEntity().asVertex().getEdges(Direction.IN, Description.DESCRIBES)){
//                    System.out.println("edge: " + e.getLabel());
//                }
//                assertEquals(d.getEntity(), unit);
//        }


//TODO: find out why the unit and the action are not connected ...
//            Iterable<Action> actions = unit.getHistory();
//            assertEquals(1, toList(actions).size());
        // Check we've only got one action
        assertEquals(1, log.getCreated());
        assertTrue(log.getAction() instanceof Action);
        assertEquals(logMessage, log.getAction().getLogMessage());

        // Ensure the import action has the right number of subjects.
        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        assertEquals(1, subjects.size());
        assertEquals(log.getSuccessful(), subjects.size());


        System.out.println("created: " + log.getCreated());

    }
}
