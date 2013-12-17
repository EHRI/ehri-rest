/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.ItsEadHandler;

/**
 * Import command for BundesArchiv EAD files. 
 * This imports files using the BundesarchiveEadHandler and BundesarchiveEadImporter.
 * @author ben
 *
 */
public class ItsEadImport extends EadImport {
	final static String NAME = "its-ead-import";
	
	public ItsEadImport() {
		super(ItsEadHandler.class, IcaAtomEadImporter.class);
	}
}
