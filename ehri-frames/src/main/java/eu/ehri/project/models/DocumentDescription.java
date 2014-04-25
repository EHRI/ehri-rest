package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.DOCUMENT_DESCRIPTION)
public interface DocumentDescription extends TemporalEntity, Description {
}
