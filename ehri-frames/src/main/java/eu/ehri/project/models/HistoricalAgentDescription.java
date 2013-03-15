package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;

@EntityType(EntityClass.HISTORICAL_AGENT_DESCRIPTION)
public interface HistoricalAgentDescription extends VertexFrame, Description, AddressableEntity {
}
