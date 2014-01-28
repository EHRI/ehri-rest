/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.CegesomaEadHandler;
import eu.ehri.project.importers.EadImporter;

/**
 * @author ben
 *
 */
public class CegesomaEadImport extends EadImport {

	final static String NAME = "cegesoma-ead-import";
	/**
	 * 
	 */
	public CegesomaEadImport() {
		super(CegesomaEadHandler.class, EadImporter.class);
	}

	

}
