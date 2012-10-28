package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionGrantTarget;

@EntityType(EntityTypes.CONTENT_TYPE)
public interface ContentType extends AccessibleEntity {
}
