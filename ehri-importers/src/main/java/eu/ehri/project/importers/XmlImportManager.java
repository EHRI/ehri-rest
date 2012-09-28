package eu.ehri.project.importers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xml.sax.SAXException;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.NoItemsCreated;
import eu.ehri.project.models.Action;

abstract public class XmlImportManager implements ImportManager {

    /**
     * Import an EAD via an URL.
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param address
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     * @throws NoItemsCreated 
     */
    public Action importUrl(String logMessage, String address)
            throws IOException, SAXException, ValidationError, NoItemsCreated {
        URL url = new URL(address);
        InputStream ios = url.openStream();
        try {
            return importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    /**
     * Import an EAD file by specifying it's path.
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param filePath
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     * @throws NoItemsCreated 
     */
    public Action importFile(String logMessage, String filePath)
            throws IOException, SAXException, ValidationError, NoItemsCreated {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    abstract public Action importFile(String logMessage, InputStream ios)
            throws SAXException, IOException, ValidationError, NoItemsCreated;
}
