package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import eu.ehri.project.models.annotations.Mandatory;

public interface IdentifiableEntity extends Frame {

    public static final String IDENTIFIER_KEY = "identifier";

    @Mandatory
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();
}
