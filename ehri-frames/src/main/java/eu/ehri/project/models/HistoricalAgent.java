package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.cvoc.AuthorativeItem;

@EntityType(EntityClass.HISTORICAL_AGENT)
public interface HistoricalAgent extends AuthorativeItem, AccessibleEntity, IdentifiableEntity,
        DescribedEntity, AnnotatableEntity {

    public static final String CREATED = "created";
}
