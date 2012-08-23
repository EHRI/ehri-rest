package eu.ehri.project.persistance;

import java.util.Map;

import eu.ehri.project.exceptions.ValidationError;

public class EntityBundle {
    private Map<String,Object> data;
    public EntityBundle(Map<String,Object> data) {
        this.data = data;
    }
    
    public void validate() throws ValidationError {}
}
