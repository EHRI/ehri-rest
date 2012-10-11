package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;

/**
 * Holds information about the collection or institute, 
 * but we don't specify (in the database) what it exactly means. 
 *
 */
@EntityType("basic")
public interface BasicFramedVertex extends AccessibleEntity, AnnotatableEntity {
	// empty!
}
