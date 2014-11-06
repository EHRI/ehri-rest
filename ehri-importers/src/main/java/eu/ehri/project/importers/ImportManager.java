package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Interface for import managers. Currently empty.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public interface ImportManager {
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError;

    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, InputParseError, ValidationError;
    
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError;
}
