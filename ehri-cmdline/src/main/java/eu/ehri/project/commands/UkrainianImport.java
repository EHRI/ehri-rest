/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.UkrainianRepoImporter;

/**
 *
 * @author linda
 */
public class UkrainianImport  extends ImportCsvCommand implements Command {

    final static String NAME = "ukrainian-import";

    /**
     * Constructor.
     */
    public UkrainianImport() {
        super(UkrainianRepoImporter.class);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -scope <country-id> <csv-file> ";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import a CSV file into the graph database, using the specified"
                + sep + "scope and user.";
        return help;
    }

   
   
}
