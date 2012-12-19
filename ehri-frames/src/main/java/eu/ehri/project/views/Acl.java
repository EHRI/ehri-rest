package eu.ehri.project.views;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

/**
 * View interface for access control operations.
 * 
 * @author mike
 *
 * @param <E>
 */
public interface Acl<E extends AccessibleEntity> {

    public PermissionGrant setPermission(E entity, Accessor user,
            PermissionType permission) throws PermissionDenied, ValidationError,
            SerializationError;
    
    public void setGlobalPermissionMatrix(Accessor accessor, Accessor grantee,
            Map<ContentTypes, List<PermissionType>> permissionMap) throws PermissionDenied,
            ValidationError, SerializationError, ItemNotFound;
    
    public void setAccessors(E entity, Set<Accessor> accessors, Accessor user)
            throws PermissionDenied;
}
