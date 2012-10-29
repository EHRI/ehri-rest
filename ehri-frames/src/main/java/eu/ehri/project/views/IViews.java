package eu.ehri.project.views;

import java.util.Map;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;

public interface IViews<E extends AccessibleEntity> {
    public void setScope(AccessibleEntity scope);
    
    public E detail(Long item, Long user) throws PermissionDenied;

    public E update(Map<String, Object> data, Long user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public E create(Map<String, Object> data, Long user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Integer delete(Long item, Long user) throws PermissionDenied,
            ValidationError, SerializationError;
}
