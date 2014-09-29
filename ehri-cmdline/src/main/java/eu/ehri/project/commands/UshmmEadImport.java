package eu.ehri.project.commands;

import eu.ehri.project.importers.EadImporter;
import eu.ehri.project.importers.UshmmHandler;

/**
 * Import command for USHMM EAD files. 
 * This imports files using the UshmmEadHandler and IcaAtomEadImporter.
 *
 * @author Ben Companjen (http://github.com/bencomp)
 */
public class UshmmEadImport extends EadImport {
	final static String NAME = "ushmm-ead-import";
	
	public UshmmEadImport() {
		super(UshmmHandler.class, EadImporter.class);
	}
}
