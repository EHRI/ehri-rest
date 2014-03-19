package eu.ehri.project.importers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Test stub for importing two EAD files describing the same documentary unit in different languages.
 * @author ben
 *
 */
public class MultilingualEadTest extends AbstractImporterTest {

	protected final String TEST_REPO = "r1";
    protected final String XMLFILENL = "archief-8224-nl.xml";
    protected final String XMLFILEFR = "archief-8224-fr.xml";
    protected final String ARCHDESC = "AA 1593",
            C01 = "cegesomaID-CEGESOMA_8224-1",
            C02_01 = "AA 1134 / 32",
            C02_02 = "AA 1134 / 34";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;
    int origCount=0;
	
	/**
	 * Import a Dutch archival description and then a French archival description
	 * of the same Cegesoma fonds and check that two description nodes were
	 * created and linked to the same documentary unit
	 */
	@Ignore
	@Test
	public void cegesomaTwoLanguageTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
		
		// setup
		PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        String logMessageNld = "Importing an example Dutch Cegesoma EAD";
        String logMessageFre = "Importing French Cegesoma EAD describing same unit";


        ImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, CegesomaEadHandler.class);
        // check conditions before imports
        origCount = getNodeCount(graph);
        //        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 5 more DocumentaryUnits (archdesc, 1 c01, 3 c02)
       	// - 5 more DocumentDescription
        // - 6 more DatePeriod
        // - 32 more subjectAccess nodes
        // - 1 more corporateBodyAccess
        // - 1 more creatorAccess
        // - 6 more import Event links (1 for each Unit, 1 for the User)
        // - 1 more import Event
        // --- = + 57, needs to be + 59
        
		
		// import Dutch description
        InputStream nlios = ClassLoader.getSystemResourceAsStream(XMLFILENL);
        ImportLog nllog = importManager.importFile(nlios, logMessageNld);

		
		// check conditions between imports
		// - new nodes:
		//   - 1 DocumentaryUnit
		//   - 1 DocumentDescription
		//   - x other nodes
		printGraph(graph);
		int newCount = origCount + 24 + 34 + 1;
        assertEquals(newCount, getNodeCount(graph));
        
		// import French description
        InputStream frios = ClassLoader.getSystemResourceAsStream(XMLFILENL);
        ImportLog frlog = importManager.importFile(frios, logMessageFre);

		
		// check conditions after imports
		// - new nodes:
		//   - 0 DocumentaryUnit
		//   - 1 DocumentDescription
		//   - x other nodes (ideally translations of subject terms 
		//          are descriptions of existing subject nodes)
		fail("Not yet implemented");
	}

}
