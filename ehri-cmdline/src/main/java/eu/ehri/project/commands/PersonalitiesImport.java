package eu.ehri.project.commands;

import eu.ehri.project.importers.PersonalitiesImporter;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class PersonalitiesImport extends ImportCsvCommand implements Command {

    final static String NAME = "csv-pers-import";

    /**
     * Constructor.
     */
    public PersonalitiesImport() {
        super(PersonalitiesImporter.class);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -scope <scope-id> <csv-file1> " +
                "<csv-file2> ... <csv-fileN>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        return "Import a CSV file as Personalities into the graph database, using the specified"
                + sep + "scope and user.";
    }

   
   
}
