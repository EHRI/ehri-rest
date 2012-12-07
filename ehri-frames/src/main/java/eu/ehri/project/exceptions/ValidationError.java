package eu.ehri.project.exceptions;

import org.apache.commons.collections.map.MultiValueMap;

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
    private MultiValueMap errors; 

    public ValidationError(String message) {        
        super(message);
        errors = new MultiValueMap();
        errors.put("item", message);
    }

    public ValidationError(Bundle bundle,
            MultiValueMap errors) {
        this(formatErrors(bundle.getClass().getName(), errors));
        this.errors = errors;
    }

    public ValidationError(Class<?> cls, MultiValueMap errors) {
        this(formatErrors(cls.getName(), errors));
        this.errors = errors;
    }

    private static String formatErrors(String clsName, MultiValueMap errors) {
        StringBuilder buf = new StringBuilder(String.format(
                "A validation error occurred building %s:\n", clsName));
        for (Object key : errors.keySet()) {
            for (Object value : errors.getCollection(key)) {
                buf.append(String.format(" - %-20s: %s", key, value));
            }
        }
        return buf.toString();
    }
    
    public MultiValueMap getErrors() {
        return errors;
    }
}
