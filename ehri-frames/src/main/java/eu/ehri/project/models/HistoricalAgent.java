package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.cvoc.AuthoritativeItem;

@EntityType(EntityClass.HISTORICAL_AGENT)
public interface HistoricalAgent extends AuthoritativeItem, AccessibleEntity, IdentifiableEntity,
        DescribedEntity, AnnotatableEntity, Watchable {

}
