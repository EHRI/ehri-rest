package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.SAXException;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.exceptions.NoItemsCreated;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;

public class InvalidEadImporterTest extends AbstractFixtureTest {

    protected final String INVALID_EAD = "invalid-ead.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    // Depends on single-ead.xml
    protected final String IMPORTED_ITEM_ID = "C00001";

    @Test(expected=ValidationError.class)
    public void testImportItemsT() throws ValidationError, SAXException, IOException, NoItemsCreated {
        UserProfile user = graph.frame(graph.getVertex(validUserId),
                UserProfile.class);
        Agent agent = graph.frame(helper.getTestVertex(TEST_REPO), Agent.class);
        final String logMessage = "Importing an invalid EAD";

        int count = getNodeCount();

        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);
        Action action;
        try {
            action = new EadImportManager(graph, agent, user).importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    @Test(expected=NoItemsCreated.class)
    public void testTolerantImport() throws ValidationError, SAXException, IOException, NoItemsCreated {
        UserProfile user = graph.frame(graph.getVertex(validUserId),
                UserProfile.class);
        Agent agent = graph.frame(helper.getTestVertex(TEST_REPO), Agent.class);
        final String logMessage = "Importing an invalid EAD";

        EadImportManager manager = new EadImportManager(graph, agent, user);
        manager.setTolerant(true);        
        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);
        try {            
            manager.importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    @Test(expected=NoItemsCreated.class)
    public void testRollback() throws ValidationError, SAXException, IOException, NoItemsCreated {
        UserProfile user = graph.frame(graph.getVertex(validUserId),
                UserProfile.class);
        Agent agent = graph.frame(helper.getTestVertex(TEST_REPO), Agent.class);
        final String logMessage = "Importing an invalid EAD";

        int count = getNodeCount();
        EadImportManager manager = new EadImportManager(graph, agent, user);
        manager.setTolerant(true);        
        InputStream ios = ClassLoader.getSystemResourceAsStream(INVALID_EAD);
        try {            
            manager.importFile(logMessage, ios);
        } catch (ValidationError e) {
            // we knew this would happen
        } finally {
            ios.close();
        }
        // Check the state of the graph is the same...
        assertEquals(count, getNodeCount());
    }
    
    
    private int getNodeCount() {
        // Note: deprecated use of getAllNodes...
        return toList(graph.getBaseGraph().getRawGraph().getAllNodes()).size();
    }

}
