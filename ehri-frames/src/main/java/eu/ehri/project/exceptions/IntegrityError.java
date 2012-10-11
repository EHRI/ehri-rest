package eu.ehri.project.exceptions;

import java.util.HashMap;
import java.util.Map;

public class IntegrityError extends Exception {
    
    private Map<String,String> fields = new HashMap<String, String>();
    private String index;
    
    public IntegrityError(String message, String index) {
        super(message);
        this.index = index;
    }
    
    public IntegrityError(String index, Map<String,String> fields) {
        super("Integity error for index: " + index);
        this.index = index;
        this.fields = fields;                
    }

    public String getIndex() {
        return index;
    }
    
    public Map<String,String> getFields() {
        return fields;
    }

    private static final long serialVersionUID = -5625119780401587251L;

}
