package eu.ehri.project.views;

import java.util.Map;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;

public interface IViews<E extends VertexFrame> {
    public E detail(long item, long user) throws PermissionDenied;

    public E update(Map<String, Object> data, long user)
            throws PermissionDenied, ValidationError, DeserializationError;

    public E create(Map<String, Object> data, long user)
            throws PermissionDenied, ValidationError, DeserializationError;

    public Integer delete(long item, long user) throws PermissionDenied,
            ValidationError, SerializationError;
}
