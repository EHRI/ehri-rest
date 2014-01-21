package eu.ehri.project.commands;

import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.UshmmHandler;

/**
 * Import command for USHMM EAD files. 
 * This imports files using the UshmmEadHandler and IcaAtomEadImporter.
 * @author ben
 *
 */
public class UshmmEadImport extends EadImport {
	final static String NAME = "ushmm-ead-import";
	
	public UshmmEadImport() {
		super(UshmmHandler.class, IcaAtomEadImporter.class);
	}
}
