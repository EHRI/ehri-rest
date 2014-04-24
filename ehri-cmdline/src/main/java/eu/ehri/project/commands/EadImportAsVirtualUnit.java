/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.EadAsVirtualUnitImporter;
import eu.ehri.project.importers.IcaAtomEadHandler;

/**
 *
 * @author linda
 */
public class EadImportAsVirtualUnit extends EadImport {
	final static String NAME = "ead-import-as-virtualunit";
	
	public EadImportAsVirtualUnit() {
		super(IcaAtomEadHandler.class, EadAsVirtualUnitImporter.class);
	}
    
        @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <user-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }
        
        @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import an EAD file into the graph database, using the specified"
                + sep + " User.";
        return help;
    }
}
