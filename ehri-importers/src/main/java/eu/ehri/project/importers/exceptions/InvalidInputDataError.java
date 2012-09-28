package eu.ehri.project.importers.exceptions;

public class InvalidInputDataError extends Exception {
    private static final long serialVersionUID = 8729453367263147233L;

    public InvalidInputDataError(String msg) {
        super(msg);
    }
}
