package eu.ehri.project.importers.exceptions;

/**
 * Indicatee that an EAD document contains an excepted document type.
 * 
 * @author michaelb
 *
 */
public class InvalidEadDocument extends Exception {
    private static final long serialVersionUID = -3771058149749123884L;

    public InvalidEadDocument(String type) {
        super("Invalid EAD document type '" + type + "'");
    }
}
