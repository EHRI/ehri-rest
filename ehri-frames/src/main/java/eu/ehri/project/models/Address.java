package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.VersionedEntity;

@EntityType(EntityClass.ADDRESS)
public interface Address extends VersionedEntity {

    @Adjacency(label = AddressableEntity.HAS_ADDRESS, direction = Direction.IN)
    public AgentDescription getAgentDescription();
    
    public static final String ADDRESS_NAME = "name";
    
    @Property(ADDRESS_NAME)
    public String getStreetAddress();
}
