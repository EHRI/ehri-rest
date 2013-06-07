/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;


import java.io.InputStream;

import eu.ehri.project.models.base.PermissionScope;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class IcaAtomJohnCageTest extends AbstractImporterTest {

    protected final String JOHNCAGEXML = "johnCage.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String FONDS_LEVEL = "Ctop level fonds";

    public IcaAtomJohnCageTest() {
    }

    @Test
    public void testImportItemsT() throws Exception {

        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(JOHNCAGEXML);
        ImportLog log = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, IcaAtomEadHandler.class).importFile(ios, logMessage);
        printGraph(graph);
        
         // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits
       	// - 1 more DocumentDescription
	// - 1 more DatePeriod
        // - 1 more UnknownProperty
        // - 1 more creatorAccess relation        
	// - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 4 UndeterminedRelationships
        assertEquals(count + 12, getNodeCount(graph));
    }
}
