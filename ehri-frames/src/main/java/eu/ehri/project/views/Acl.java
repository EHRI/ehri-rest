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
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

/**
 * View interface for access control operations.
 * 
 * @author mike
 * 
 */
public interface Acl {

    public void setGlobalPermissionMatrix(Accessor accessor,
            Map<ContentTypes, List<PermissionType>> permissionMap,
            Accessor grantee) throws PermissionDenied, ValidationError,
            SerializationError, ItemNotFound;

    public void setItemPermissions(AccessibleEntity entity, Accessor accessor,
            Set<PermissionType> permissions, Accessor grantee)
            throws PermissionDenied;

    public void setAccessors(AccessibleEntity entity, Set<Accessor> accessors,
            Accessor user) throws PermissionDenied;

    public void revokePermissionGrant(PermissionGrant grant, Accessor user)
            throws PermissionDenied;

    public void addAccessorToGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied;

    public void removeAccessorFromGroup(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied;
}
