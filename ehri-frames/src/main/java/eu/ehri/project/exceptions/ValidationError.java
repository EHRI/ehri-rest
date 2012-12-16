package eu.ehri.project.exceptions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eu.ehri.project.persistance.Bundle;

/**
 * Validation error. This exception holds a map
 * of field=error(s) values.
 *
 * @author michaelb
 *
 */
public class ValidationError extends Exception {

    private static final long serialVersionUID = 1L;
    private ListMultimap<String, String> errors; 

    public ValidationError(String message) {        
        super(message);
        errors = ArrayListMultimap.create();
        errors.put("item", message);
    }

    public ValidationError(Bundle bundle,
            ListMultimap<String, String> errors) {
        this(formatErrors(bundle.getClass().getName(), errors));
        this.errors = errors;
    }

    public ValidationError(Class<?> cls, ListMultimap<String, String> errors) {
        this(formatErrors(cls.getName(), errors));
        this.errors = errors;
    }

    private static String formatErrors(String clsName, ListMultimap<String, String> errors) {
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
}
