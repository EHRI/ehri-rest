package eu.ehri.project.models;

import com.tinkerpop.frames.*;

import eu.ehri.project.relationships.*;

public interface Entity {

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

    @Incidence(label = "access")
    public Iterable<Access> getAccess();

    @Adjacency(label = "access")
    public Iterable<Accessor> getAccessibleTo();

    @Adjacency(label = "access")
    public void addAccessor(final Accessor accessor);
}
