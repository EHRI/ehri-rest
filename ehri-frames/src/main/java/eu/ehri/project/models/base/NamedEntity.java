package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import eu.ehri.project.models.annotations.Mandatory;

public interface NamedEntity extends Frame {

    public static final String NAME = "name";

    @Mandatory
    @Property(NAME)
    public String getName();
}
