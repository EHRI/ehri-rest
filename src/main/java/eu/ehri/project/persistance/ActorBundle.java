package eu.ehri.project.persistance;

import java.util.Map;

import eu.ehri.project.exceptions.ValidationError;

public class ActorBundle extends EntityBundle {
    public ActorBundle(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void validate() throws ValidationError {
        
    }
}
