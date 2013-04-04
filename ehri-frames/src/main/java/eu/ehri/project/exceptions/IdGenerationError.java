package eu.ehri.project.exceptions;

import java.util.Map;

import eu.ehri.project.models.base.Frame;

public class IdGenerationError extends Exception {
    
    public IdGenerationError(String prefix, Frame scope, Map<String, Object> data) {
        super(String.format("Error generating ID for type prefix '%s': [Data: %s, Scope: %s]", prefix, scope, data));
    }    

    public IdGenerationError(String err, String prefix, Frame scope, Map<String, Object> data) {
        super(String.format("%s [Prefix: %s, Data: %s, Scope: %s]", err, prefix, scope, data));
    }    
    
    private static final long serialVersionUID = -5625119780401587251L;
}
