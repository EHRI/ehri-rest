/**
 * 
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.NiodEadHandler;

/**
 * @author ben
 *
 */
public class NiodEadImport extends EadImport {

	final static String NAME = "niod-ead-import";
	/**
	 * 
	 */
	public NiodEadImport() {
		super(NiodEadHandler.class, IcaAtomEadImporter.class);
	}

	

}
