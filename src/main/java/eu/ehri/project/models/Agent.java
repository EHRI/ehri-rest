package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityTypes.AGENT)
public interface Agent extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {

    public static final String HOLDS = "holds";
    public static final String HAS_ADDRESS = "hasAddress";

    @Adjacency(label = HOLDS)
    public Iterable<DocumentaryUnit> getCollections();
    
    @Adjacency(label= HAS_ADDRESS)
    public Iterable<Address> getAddresses();
    
    @Adjacency(label= HAS_ADDRESS)
    public void addAddress(final Address address); 
   
    @Adjacency(label = HOLDS)
    public void addCollection(final TemporalEntity collection);
}
