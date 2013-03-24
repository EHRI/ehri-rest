package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;

public interface NamedEntity extends Frame {

    public static final String NAME = "name";

    @Property(NAME)
    public String getName();
}
