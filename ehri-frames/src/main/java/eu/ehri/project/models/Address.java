package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.LocatedEntity;

@EntityType(EntityClass.ADDRESS)
public interface Address extends AnnotatableEntity, LocatedEntity {
}
