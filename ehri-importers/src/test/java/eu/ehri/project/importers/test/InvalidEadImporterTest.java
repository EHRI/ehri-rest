package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.test.AbstractFixtureTest;

public class InvalidEadImporterTest extends AbstractFixtureTest {

    protected final String INVALID_EAD = "invalid-ead.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    // Depends on single-ead.xml
    protected final String IMPORTED_ITEM_ID = "C00001";

    @Test(expected=ValidationError.class)
    public void testImportItemsT() throws ValidationError, IOException, InputParseError, ItemNotFound {
        Agent agent = manager.getFrame(TEST_REPO, Agent.class);
        final String logMessage = "Importing an invalid EAD";
        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);

        try {
            new EadImportManager(graph, agent, validUser).importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    @Test
    public void testTolerantImport() throws ValidationError, IOException, InputParseError, ItemNotFound {
        Agent agent = manager.getFrame(TEST_REPO, Agent.class);
        final String logMessage = "Importing an invalid EAD";

        EadImportManager manager = new EadImportManager(graph, agent, validUser);
        manager.setTolerant(true);        
        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);
        try {            
            ImportLog log = manager.importFile(ios, logMessage);
            assertFalse(log.isValid());
            assertEquals(1, log.getErrored());
        } finally {
            ios.close();
        }
    }

    @Test
    public void testRollback() throws ValidationError, IOException, InputParseError, ItemNotFound {
        Agent agent = manager.getFrame(TEST_REPO, Agent.class);
        final String logMessage = "Importing an invalid EAD";

        int count = getNodeCount();
        EadImportManager manager = new EadImportManager(graph, agent, validUser);
        manager.setTolerant(true);        
        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);
        ImportLog log = manager.importFile(ios, logMessage);

        // Check the state of the graph is the same...
        assertEquals(count, getNodeCount());
        assertEquals(1, log.getErrored());
    }
    
    
    private int getNodeCount() {
        // Note: deprecated use of getAllNodes...
        return toList(graph.getBaseGraph().getRawGraph().getAllNodes()).size();
    }

}
