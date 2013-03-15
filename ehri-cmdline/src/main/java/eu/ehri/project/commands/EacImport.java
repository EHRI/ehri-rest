/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.EacHandler;
import eu.ehri.project.importers.EacImporter;

/**
 *
 * @author linda
 */
public class EacImport extends ImportCommand implements Command {

    final static String NAME = "eac-import";

    /**
     * Constructor.
     */
    public EacImport() {
        super(EacHandler.class, EacImporter.class);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -repo <agent-id> <eac1.xml> <eac2.xml> ... <eacN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import an EAC file into the graph database, using the specified"
                + sep + "Repository and User.";
        return help;
    }

   
   
}
