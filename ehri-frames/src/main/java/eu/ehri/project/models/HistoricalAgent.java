package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.cvoc.AuthoritativeItem;

/**
 * A frame class representing a historical agent item.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.HISTORICAL_AGENT)
public interface HistoricalAgent extends AuthoritativeItem, AccessibleEntity, IdentifiableEntity,
        DescribedEntity, AnnotatableEntity, Watchable {
}
