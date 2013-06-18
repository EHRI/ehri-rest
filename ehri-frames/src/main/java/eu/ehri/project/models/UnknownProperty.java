package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Holds information about the collection or institute, 
 * but we don't specify (in the database) what it exactly means. 
 *
 */
@EntityType(EntityClass.UNKNOWN_PROPERTY)
public interface UnknownProperty extends AccessibleEntity {
}
