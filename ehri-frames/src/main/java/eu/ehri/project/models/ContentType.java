package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Frame class representing a type of content in the
 * graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.CONTENT_TYPE)
public interface ContentType extends AccessibleEntity {
}
