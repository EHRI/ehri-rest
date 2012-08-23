package eu.ehri.project.persistance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;


import eu.ehri.project.exceptions.ValidationError;

public final class EntityBundleFactory<T extends EntityBundle> {
    /*
     * Takes some data and constructs a bundle, running the types
     * validation routes to ensure the data fulfils the type contract.
     */
    public T buildBundle(Map<String, Object> data, Class<T> cls) throws ValidationError {
        try {
            Constructor<T> constructor = cls.getConstructor(Map.class);            
            T bundle = constructor.newInstance(data);
            bundle.validate();
            return bundle;
        } catch (InstantiationException e) {
            // What the hell are we supposed to do to handle this?
            throw new RuntimeException("Something went badly wrong!", e);
        } catch (IllegalAccessException e) {
            // Ditto...
            throw new RuntimeException("Something went badly wrong!", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Something went badly wrong!", e);
        } catch (SecurityException e) {
            throw new RuntimeException("Something went badly wrong!", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Something went badly wrong!", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Something went badly wrong!", e);
        }
    }
}
