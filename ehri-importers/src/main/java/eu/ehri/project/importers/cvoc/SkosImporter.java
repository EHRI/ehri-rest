package eu.ehri.project.importers.cvoc;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface SkosImporter {

    /**
     * Import a file with a given log message.
     *
     * @param filePath   The file path
     * @param logMessage A log message
     * @return An import log
     * @throws IOException
     * @throws InputParseError
     * @throws ValidationError
     */
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError;

    /**
     * Import an input stream with a given log message.
     *
     * @param ios        The input stream
     * @param logMessage A log message
     * @return An import log
     * @throws IOException
     * @throws InputParseError
     * @throws ValidationError
     */
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, InputParseError, ValidationError;

    /**
     * Switch the importer mode to one that is tolerant
     * of individual item validation errors.
     *
     * @param tolerant Allow individual items to error
     * @return A new SKOS importer with the given tolerance mode
     */
    public SkosImporter setTolerant(boolean tolerant);

    /**
     * Set the RDF format. Supported values are: N3, TTL, TURTLE,
     * and the default, RDF/XML.
     *
     * @param format The RDF format string.
     * @return A new SKOS importer with the given format
     */
    public SkosImporter setFormat(String format);

    /**
     * Set default language for literals without a lang suffix.
     *
     * @param lang A two- or three-letter language code
     * @return A new SKOS importer with the given default language
     */
    public SkosImporter setDefaultLang(String lang);
}
