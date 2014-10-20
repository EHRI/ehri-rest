package eu.ehri.project.commands;

import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.VirtualEadHandler;
import eu.ehri.project.importers.VirtualEadImporter;
import eu.ehri.project.importers.SaxXmlHandler;

/**
 * Import EAD from the command line...
 * 
 */
public class EadAsVirtualCollectionImport extends ImportCommand implements Command {

    final static String NAME = "virtual-ead-import";

    /**
     * Constructor.
     */
    public EadAsVirtualCollectionImport() {
        this(VirtualEadHandler.class, VirtualEadImporter.class);
    }
    
    /**
     * Generic EAD import command, designed for extending classes that use specific Handlers.
     * @param handler The Handler class to be used for import
     * @param importer The Importer class to be used. If null, IcaAtomEadImporter is used.
     */
    public EadAsVirtualCollectionImport(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer) {
    	super(handler, importer);
    }

        @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <importing-user-id> -scope <responsible-user-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import an EAD file into the graph database as a tree of VirtualUnits, using the specified"
                + sep + "responsible User and importing User.";
        return help;
    }
}
