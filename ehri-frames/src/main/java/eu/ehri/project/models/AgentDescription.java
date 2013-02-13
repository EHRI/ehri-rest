package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Description;

@EntityType(EntityClass.AGENT_DESCRIPTION)
public interface AgentDescription extends Description {

    public static final String HAS_ADDRESS = "hasAddress";

    @Fetch(HAS_ADDRESS)
    @Dependent
    @Adjacency(label = HAS_ADDRESS)
    public Iterable<Address> getAddresses();

    @Adjacency(label = HAS_ADDRESS)
    public void addAddress(final Address address);
}
