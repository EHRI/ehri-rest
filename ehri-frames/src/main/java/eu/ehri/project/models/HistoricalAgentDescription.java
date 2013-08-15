package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.HISTORICAL_AGENT_DESCRIPTION)
public interface HistoricalAgentDescription extends Description, TemporalEntity {
}
