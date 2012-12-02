package eu.ehri.project.views;

import java.util.Map;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

public interface Crud<E extends AccessibleEntity> {
    public Crud<E> setScope(PermissionScope scope);
    
    public E detail(E item, Accessor user) throws PermissionDenied;

    public E update(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public E create(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError;
}
