/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.EadImporter;
import eu.ehri.project.importers.ItsEadHandler;

/**
 * Import command for ITS EAD files. 
 * This imports files using the ItsEadHandler and IcaAtomEadImporter.
 * @author ben
 *
 */
public class ItsEadImport extends EadImport {
	final static String NAME = "its-ead-import";
	
	public ItsEadImport() {
		super(ItsEadHandler.class, EadImporter.class);
	}
}
