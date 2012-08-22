package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface Agent extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {

    public static final String isA = "agent";
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
