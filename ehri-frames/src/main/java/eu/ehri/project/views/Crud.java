package eu.ehri.project.views;

import com.tinkerpop.frames.VertexFrame;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;

public interface Crud<E extends AccessibleEntity> {
    public Crud<E> setScope(PermissionScope scope);
    
    public E detail(E item, Accessor user) throws AccessDenied;

    public E update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound;

    public <T extends VertexFrame> T updateDependent(Bundle bundle, E parent, Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound;

    public E create(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public <T extends VertexFrame> T createDependent(Bundle bundle, E parent, Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public E createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError;

    public <T extends VertexFrame> Integer deleteDependent(T item, E parent, Accessor user, Class<T> dependentClass)
            throws PermissionDenied, ValidationError, SerializationError;
}
