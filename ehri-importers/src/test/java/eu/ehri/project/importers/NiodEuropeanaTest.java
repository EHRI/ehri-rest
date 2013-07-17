/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class NiodEuropeanaTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(IcaAtomEadImporterTest.class);
       protected final String SINGLE_EAD = "niod_record.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String UNIT_IDENTIFIER = "T1974-016";

    @Test
    public void testImportItemsT() throws Exception {

         Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single Niod Europeana record";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new SaxImportManager(graph, agent, validUser, NiodEuropeanaImporter.class, NiodEuropeanaHandler.class).setTolerant(Boolean.FALSE).importFile(ios, logMessage);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits
       	// - 1 more DocumentDescription
	// - 1 more DatePeriod
        // - 1 more UnknownProperty
	// - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        assertEquals(count + 7, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY,
                UNIT_IDENTIFIER);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds_unit = graph.frame(
                getVertexByIdentifier(graph,UNIT_IDENTIFIER),
                DocumentaryUnit.class);

        for(Description d : fonds_unit.getDescriptions()) {
            assertEquals("Jodenster", d.getName());
        }


        // Ensure the import action has the right number of subjects.
//        Iterable<Action> actions = unit.getHistory();
        // Check we've created 4 items
        assertEquals(1, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());


        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for(AccessibleEntity subject  : subjects)
            logger.info("identifier: " + subject.getId());
        
        assertEquals(1, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        for (AccessibleEntity e : log.getAction().getSubjects()) {
            assertEquals(agent, e.getPermissionScope());
        }
    }

}
