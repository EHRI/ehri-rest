/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.PersonalitiesImporter;

/**
 *
 * @author linda
 */
public class PersonalitiesImport extends ImportCsvCommand implements Command {

    final static String NAME = "personalities-import";

    /**
     * Constructor.
     */
    public PersonalitiesImport() {
        super(PersonalitiesImporter.class);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -scope <authorative-set-id> <personalities.csv>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import the Personalities file into the graph database, using the specified"
                + sep + "AuthorativeSet and User.";
        return help;
    }

   
   
}
