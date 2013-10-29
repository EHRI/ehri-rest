package eu.ehri.project.commands;

import eu.ehri.project.importers.BundesarchiveEadHandler;
import eu.ehri.project.importers.IcaAtomEadHandler;
import eu.ehri.project.importers.IcaAtomEadImporter;

/**
 * Import EAD from the command line...
 * 
 */
public class EadImport extends ImportCommand implements Command {

    final static String NAME = "ead-import";

    /**
     * Constructor.
     */
    public EadImport() {
        super(IcaAtomEadHandler.class, IcaAtomEadImporter.class);
    }

    

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <user-id> -scope <agent-id> <neo4j-graph-dir> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import an EAD file into the graph database, using the specified"
                + sep + "Repository and User.";
        return help;
    }
}
