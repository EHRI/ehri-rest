package eu.ehri.project.persistance;

import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import eu.ehri.project.exceptions.ValidationError;

public class BundleFactory {

    public Bundle buildBundle(Map<String, Object> data, Class<?> cls)
            throws ValidationError {
        return buildBundle(data, cls, new MultiValueMap());
    }

    public Bundle buildBundle(Map<String, Object> data, Class<?> cls,
            MultiValueMap saveWith) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.
        return new Bundle(null, data, cls, saveWith);
    }

    public Bundle buildBundle(String id, Map<String, Object> data,
            Class<?> cls) throws ValidationError {
        return buildBundle(id, data, cls, new MultiValueMap());
    }

    public Bundle buildBundle(String id, Map<String, Object> data,
            Class<?> cls, MultiValueMap saveWith) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.
        return new Bundle(id, data, cls, saveWith);
    }
}
