package eu.ehri.project.importers.exceptions;

public class InvalidInputFormatError extends Exception {
    private static final long serialVersionUID = 8729453367263147233L;

    public InvalidInputFormatError(String msg) {
        super(msg);
    }
}
