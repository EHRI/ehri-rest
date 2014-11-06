package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AnnotatableEntity;

/**
 * A frame class representing an address.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.ADDRESS)
public interface Address extends AnnotatableEntity {
}
