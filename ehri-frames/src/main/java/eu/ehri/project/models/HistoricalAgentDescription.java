package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Frame class representing the description of a historical
 * agent.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.HISTORICAL_AGENT_DESCRIPTION)
public interface HistoricalAgentDescription extends Description, TemporalEntity {
}
