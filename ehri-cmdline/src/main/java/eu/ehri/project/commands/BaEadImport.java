/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.BundesarchiveEadHandler;
import eu.ehri.project.importers.BundesarchiveEadImporter;

/**
 * Import command for BundesArchiv EAD files. 
 * This imports files using the BundesarchiveEadHandler and BundesarchiveEadImporter.
 * @author ben
 *
 */
public class BaEadImport extends EadImport {
	final static String NAME = "ba-ead-import";
	
	public BaEadImport() {
		super(BundesarchiveEadHandler.class, BundesarchiveEadImporter.class);
	}
}
