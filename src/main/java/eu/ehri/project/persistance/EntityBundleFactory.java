package eu.ehri.project.persistance;

import java.util.Map;


import eu.ehri.project.exceptions.ValidationError;

public final class EntityBundleFactory <T extends EntityBundle> {
    /*
     * Takes some data and constructs a bundle, running the types
     * validation routes to ensure the data fulfils the type contract.
     */
    public T buildBundle(Map<String, Object> data, Class<T> cls) throws ValidationError {
        try {
            T bundle = cls.getConstructor(Map.class).newInstance(data);            
            bundle.validate();
            return bundle;
        } catch (Exception e) {
            // What the hell are we supposed to do to handle this raft of
            // obscure reflection-related errors. Better to bail out and
            // pass on the cause.
            throw new RuntimeException(String.format("Error creating bundle class: '%s'", cls), e);
        }
    }
}
