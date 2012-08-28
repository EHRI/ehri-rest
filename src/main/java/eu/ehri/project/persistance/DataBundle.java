package eu.ehri.project.persistance;

import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;

public interface DataBundle<T extends VertexFrame> {
    public Class<T> getBundleClass();

    public MultiValueMap getValidationErrors();

    public DataBundle<T> setDataValue(String key, Object value)
            throws ValidationError;

    public DataBundle<T> setData(final Map<String, Object> data);

    void validate() throws ValidationError;
}
