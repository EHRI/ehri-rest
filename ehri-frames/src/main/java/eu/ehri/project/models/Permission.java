package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Frame class representing a type of permission. These
 * have the same names as the values in the {@link eu.ehri.project.acl.PermissionType}
 * enum.
 */
@EntityType(EntityClass.PERMISSION)
public interface Permission extends AccessibleEntity {
}
