package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface NamedEntity extends VertexFrame {

    public static final String NAME = "name";

    @Property(NAME)
    public String getName();
}
