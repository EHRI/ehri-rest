package eu.ehri.project.importers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;

/**
 * Interface for import managers. Currently empty.
 * 
 * @author michaelb
 * 
 */
public interface ImportManager {
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError;

    public ImportLog importUrl(String url, String logMessage)
            throws IOException, InputParseError, ValidationError;
    
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, InputParseError, ValidationError;
    
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError;

    public ImportManager addCallback(ImportCallback cb);
}
