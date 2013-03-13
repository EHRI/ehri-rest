package eu.ehri.project.models;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;

@EntityType(EntityClass.AGENT_DESCRIPTION)
public interface AgentDescription extends Description, AddressableEntity {

    
}
