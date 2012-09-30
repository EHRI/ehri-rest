package eu.ehri.project.importers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.NoItemsCreated;

abstract public class XmlImportManager implements ImportManager {

    /**
     * Import an EAD via an URL.
     * 
     * @param address
     * @param logMessage
     * @param graph
     * @param agent
     * @param actioner
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     * @throws NoItemsCreated
     */
    public ImportLog importUrl(String address, String logMessage)
            throws IOException, InputParseError, ValidationError,
            NoItemsCreated {
        URL url = new URL(address);
        InputStream ios = url.openStream();
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    /**
     * Import an EAD file by specifying it's path.
     * 
     * @param filePath
     * @param logMessage
     * @param graph
     * @param agent
     * @param actioner
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     * @throws NoItemsCreated
     */
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError,
            NoItemsCreated {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    abstract public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, NoItemsCreated,
            InputParseError;
}
