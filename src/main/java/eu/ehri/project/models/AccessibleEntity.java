package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.relationships.Access;

public interface AccessibleEntity {

    public static final String ACCESS = "access";

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Property("element_type")
    public String getType();

    @Property("identifier")
    public String getIdentifier();

    @Property("identifier")
    public String setIdentifier();

    @Incidence(label = ACCESS)
    public Iterable<Access> getAccess();

    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessibleTo();

    @Adjacency(label = ACCESS)
    public void addAccessor(final Accessor accessor);
}
