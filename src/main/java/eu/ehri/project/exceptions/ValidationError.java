package eu.ehri.project.exceptions;

import java.util.Map;
import java.util.Map.Entry;

import eu.ehri.project.persistance.EntityBundle;

public class ValidationError extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public ValidationError(String message) {
        super(message);
    }
    
    public ValidationError(EntityBundle bundle, Map<String, String> errors) {
        this(formatErrors(bundle, errors));
    }
    
    private static String formatErrors(EntityBundle bundle, Map<String, String> errors) {
        StringBuilder buf = new StringBuilder(
                String.format("A validation error occurred building %s:\n", bundle.toString()));
        for (Entry<String,String> entry: errors.entrySet()) {
            buf.append(String.format(" - %-20s: %s", entry.getKey(), entry.getValue()));
        }
        return buf.toString();
    }

}
