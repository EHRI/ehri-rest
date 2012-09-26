package eu.ehri.project.exceptions;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.persistance.EntityBundle;

public class ValidationError extends EhriBaseError {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ValidationError(String message) {
        super(message);
    }

    public ValidationError(EntityBundle<? extends VertexFrame> bundle,
            MultiValueMap errors) {
        this(formatErrors(bundle.getClass().getName(), errors));
    }

    public ValidationError(Class<?> cls, MultiValueMap errors) {
        this(formatErrors(cls.getName(), errors));
    }

    private static String formatErrors(String clsName, MultiValueMap errors) {
        StringBuilder buf = new StringBuilder(String.format(
                "A validation error occurred building %s:\n", clsName));
        for (Object key : errors.keySet()) {
            for (Object value : errors.getCollection(key)) {
                buf.append(String.format(" - %-20s: %s", key, value));
            }
        }
        return buf.toString();
    }
}
