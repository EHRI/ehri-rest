package eu.ehri.project.persistance;

import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;

public class BundleFactory<T extends VertexFrame> {

    public Bundle<T> buildBundle(Map<String, Object> data, Class<T> cls)
            throws ValidationError {
        return buildBundle(data, cls, new MultiValueMap());
    }

    public Bundle<T> buildBundle(Map<String, Object> data, Class<T> cls,
            MultiValueMap saveWith) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.
        return new Bundle<T>(null, data, cls, saveWith);
    }

    public Bundle<T> buildBundle(String id, Map<String, Object> data,
            Class<T> cls) throws ValidationError {
        return buildBundle(id, data, cls, new MultiValueMap());
    }

    public Bundle<T> buildBundle(String id, Map<String, Object> data,
            Class<T> cls, MultiValueMap saveWith) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.
        return new Bundle<T>(id, data, cls, saveWith);
    }
}
