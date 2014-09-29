package eu.ehri.project.importers.exceptions;

/**
 * Indicate that an XML document contains an excepted document type.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public class InvalidXmlDocument extends Exception {
    private static final long serialVersionUID = -3771058149749123884L;

    public InvalidXmlDocument(String type) {
        super("Invalid XML document type '" + type + "'");
    }
}
