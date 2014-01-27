package eu.ehri.project.importers;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test stub for importing two EAD files describing the same documentary unit in different languages.
 * @author ben
 *
 */
public class MultilingualEadTest extends AbstractImporterTest {

	
	
	/**
	 * Import a Dutch archival description and then a French archival description
	 * of the same Cegesoma fonds and check that two description nodes were
	 * created and linked to the same documentary unit
	 */
	@Ignore
	@Test
	public void cegesomaTwoLanguageTest() {
		
		// check conditions before imports
		
		
		// import Dutch description
		
		
		// check conditions between imports
		// - new nodes:
		//   - 1 DocumentaryUnit
		//   - 1 DocumentDescription
		//   - x other nodes
		
		
		// import French description
		
		
		// check conditions after imports
		// - new nodes:
		//   - 0 DocumentaryUnit
		//   - 1 DocumentDescription
		//   - x other nodes (ideally translations of subject terms 
		//          are descriptions of existing subject nodes)
		fail("Not yet implemented");
	}

}
