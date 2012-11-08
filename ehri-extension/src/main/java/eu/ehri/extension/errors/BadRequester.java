package eu.ehri.extension.errors;

public class BadRequester extends Exception {

    private static final long serialVersionUID = 2608176871477505511L;

    public BadRequester(String requester) {
        super(String.format("Requester missing or badly formed: '%s", requester));
    }
}
