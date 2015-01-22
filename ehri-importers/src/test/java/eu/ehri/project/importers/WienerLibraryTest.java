package eu.ehri.project.importers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class WienerLibraryTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(WienerLibraryTest.class);

    protected final String SINGLE_EAD = "8342_ehriID_wpath_mainids.xml";

    protected final String FONDS_LEVEL = "Ctop level fonds";
    protected final String SUBFONDS_LEVEL = "C00001";
    protected final String C2 = "C00002";
    protected final String C2_1 = "C00002-1";
    protected final String C2_2 = "C00002-2";
    /**
     * Test import of a Wiener Library EAD file.
     * @throws ItemNotFound 
     * @throws InputParseError 
     * @throws ValidationError 
     * @throws IOException 
     */
//    @Test
    public void wienerLibraryTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
//        int userActions = toList(validUser.getActions()).size();
//        logger.info("size : " + userActions);
//
//        int nodeCount = getNodeCount(graph);
//        ActionManager am = new ActionManager(graph);
//        logger.info("size : " + toList(validUser.getActions()).size());
//        ActionManager.EventContext ctx = am.logEvent(validUser,
//                EventTypes.creation,
//                Optional.of("Doing something to lots of nodes"));
//        assertEquals(nodeCount + 2, getNodeCount(graph));
//
//        assertEquals(validUser, ctx.getActioner());
//        assertEquals(userActions + 1, toList(validUser.getActions()).size());
//        logger.info("number of events on event: " + toList(ctx.getSystemEvent().getHistory()).size());
//        logger.info("size : " + toList(validUser.getActions()).size());
//        

        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, 
                new XmlImportProperties("wienerlib.properties"))
                .setTolerant(Boolean.FALSE);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more UnknownProperties
        // - 11 UndeterminedRelationship, from controlled access points
        // - 2 more import Event links (1 for the Unit, 1 for the User)
        // - 1 more import Event
        int newCount = count + 24;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY,
                FONDS_LEVEL);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds_unit = graph.frame(
                getVertexByIdentifier(graph,FONDS_LEVEL),
                DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(
                getVertexByIdentifier(graph,SUBFONDS_LEVEL),
                DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(
                getVertexByIdentifier(graph,C2),
                DocumentaryUnit.class);
        DocumentaryUnit c2_1 = graph.frame(
                getVertexByIdentifier(graph,C2_1),
                DocumentaryUnit.class);
        DocumentaryUnit c2_2 = graph.frame(
                getVertexByIdentifier(graph,C2_2),
                DocumentaryUnit.class);

        // Ensure that the first child's parent is unit
        assertEquals(c1, c2.getParent());

        // Ensure the grandkids parents is c1
        assertEquals(c2, c2_1.getParent());
        assertEquals(c2, c2_2.getParent());

        // Ensure unit the the grandparent of cc1
        List<DocumentaryUnit> ancestors = toList(c2_1.getAncestors());
        assertEquals(fonds_unit, ancestors.get(ancestors.size() - 1));

        // Ensure the import action has the right number of subjects.
//        Iterable<Action> actions = unit.getHistory();
        // Check we've created 4 items
        assertEquals(5, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());


        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for(AccessibleEntity subject  : subjects)
            logger.info("identifier: " + subject.getId());
        
        assertEquals(5, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check all descriptions have an IMPORT creationProcess
        for (Description d : c1.getDocumentDescriptions()) {
            assertEquals(Description.CreationProcess.IMPORT, d.getCreationProcess());
        }

        // Check permission scopes
        assertEquals(repository, fonds_unit.getPermissionScope());
        assertEquals(fonds_unit, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c2_1.getPermissionScope());
        assertEquals(c2, c2_2.getPermissionScope());

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(
                ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(5, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));

    }
}
