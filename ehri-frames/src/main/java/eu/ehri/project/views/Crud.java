package eu.ehri.project.views;

import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;

public interface Crud<E extends AccessibleEntity> {
    public Crud<E> setScope(PermissionScope scope);
    
    public E detail(E item, Accessor user) throws AccessDenied;

    public Mutation<E> update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound;

    public <T extends Frame, P extends DescribedEntity> Mutation<T> updateDependent(Bundle bundle, P parent,
            Accessor user,
            Class<T> dependentClass)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound;

    public E create(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public <T extends Frame, P extends DescribedEntity> T createDependent(Bundle bundle, P parent, Accessor user,
            Class<T> dependentClass)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Mutation<E> createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, IntegrityError;

    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError;

    public <T extends Frame, P extends DescribedEntity> Integer deleteDependent(T item, P parent, Accessor user,
            Class<T> dependentClass)
            throws PermissionDenied, ValidationError, SerializationError;
}
