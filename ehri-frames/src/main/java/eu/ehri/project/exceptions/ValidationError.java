package eu.ehri.project.exceptions;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import eu.ehri.project.persistance.Bundle;

/**
 * Validation error. This exception holds a map of field=error(s) values.
 * 
 * @author michaelb
 * 
 */
public class ValidationError extends BundleError {

    private static final long serialVersionUID = 1L;
    private ListMultimap<String, String> errors;
    private ListMultimap<String, BundleError> relations;

    public ValidationError(Bundle bundle, ListMultimap<String, String> errors,
            ListMultimap<String, BundleError> relations) {
        super(formatErrors(bundle.getClass().getName(), errors));
        this.errors = errors;
        this.relations = relations;
    }

    public ValidationError(Bundle bundle, String key, String error) {        
        this(bundle, errorsFromKeyValue(key, error));
    }

    public ValidationError(Bundle bundle,
            ListMultimap<String, String> errors) {
        this(bundle, errors, LinkedListMultimap.<String,BundleError>create());
    }

    private static String formatErrors(String clsName,
            ListMultimap<String, String> errors) {
        StringBuilder buf = new StringBuilder(String.format(
                "A validation error occurred building %s:\n", clsName));
        for (String key : errors.keySet()) {
            for (String value : errors.get(key)) {
                buf.append(String.format(" - %-20s: %s", key, value));
            }
        }
        return buf.toString();
    }

    public ListMultimap<String, String> getErrors() {
        return errors;
    }

    public ListMultimap<String, BundleError> getRelations() {
        return relations;
    }
    
    private static ListMultimap<String, String> errorsFromKeyValue(String key, String value) {
        ListMultimap<String, String> errors = LinkedListMultimap.create();
        errors.put(key, value);
        return errors;
    }
}
