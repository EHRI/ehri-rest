package eu.ehri.project.views;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;

public interface Crud<E extends AccessibleEntity> {
    public Crud<E> setScope(PermissionScope scope);
    
    public E detail(E item, Accessor user) throws PermissionDenied;

    public E update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound;

    public E create(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public E createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError;
}
