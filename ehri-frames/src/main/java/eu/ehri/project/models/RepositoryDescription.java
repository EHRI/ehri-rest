package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.REPOSITORY_DESCRIPTION)
public interface RepositoryDescription extends Description, AddressableEntity, TemporalEntity {
}
