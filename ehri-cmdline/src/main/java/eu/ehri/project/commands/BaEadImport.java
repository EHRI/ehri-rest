/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.BundesarchiveEadHandler;
import eu.ehri.project.importers.EadImporter;

/**
 * Import command for BundesArchiv EAD files. 
 * This imports files using the BundesarchiveEadHandler and EadImporter.
 * @author ben
 *
 */
public class BaEadImport extends EadImport {
	final static String NAME = "ba-ead-import";
	
	public BaEadImport() {
		super(BundesarchiveEadHandler.class, EadImporter.class);
	}
}
