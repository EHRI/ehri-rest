package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.Address;

@EntityType(EntityTypes.AGENT_DESCRIPTION)
public interface AgentDescription extends Description {

    public static final String HAS_ADDRESS = "hasAddress";

    @Property("name")
    public String getTitle();

    @Property("otherFormsOfName")
    public String[] otherFormsOfName();

    @Property("parallelFormsOfName")
    public String[] parallelFormsOfName();

    @Fetch
    @Dependent
    @Adjacency(label = HAS_ADDRESS)
    public Iterable<Address> getAddresses();

    @Adjacency(label = HAS_ADDRESS)
    public void addAddress(final Address address);
}
