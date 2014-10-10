package eu.ehri.project.commands;

import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.EadHandler;
import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.SaxXmlHandler;

/**
 * Import EAD from the command line...
 * 
 */
public class IcaAtomEadImport extends ImportCommand implements Command {

    final static String NAME = "ica-atom-ead-import";

    /**
     * Constructor.
     */
    public IcaAtomEadImport() {
        this(EadHandler.class, IcaAtomEadImporter.class);
    }
    
    /**
     * Generic EAD import command, designed for extending classes that use specific Handlers.
     * @param handler The Handler class to be used for import
     * @param importer The Importer class to be used. If null, IcaAtomEadImporter is used.
     */
    public IcaAtomEadImport(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer) {
    	super(handler, importer);
    }

        @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <user-id> -scope <repository-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        return "Import an EAD file into the graph database, using the specified"
                + sep + "Repository and User.";
    }
}
