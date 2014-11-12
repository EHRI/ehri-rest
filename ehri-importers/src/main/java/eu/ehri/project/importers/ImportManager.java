package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An Import Manager takes a single file or stream, or a list of files, and a log message
 * and imports the data from the file into the database, 
 * linking the import as an action with the given log message.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public interface ImportManager {
    
    /**
     * Import a file by specifying its path.
     * 
     * @param filePath         path to the file to import
     * @param logMessage       an optional message to describe the import
     * @return                 an ImportLog for the given file
     *
     * @throws IOException     when reading or writing files fails
     * @throws InputParseError when parsing the file fails
     * @throws ValidationError when the content of the file is invalid
     */
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError;

    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, InputParseError, ValidationError;
    
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError;
}
