package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Frame class representing the description of a repository.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.REPOSITORY_DESCRIPTION)
public interface RepositoryDescription extends Description, AddressableEntity, TemporalEntity {
}
