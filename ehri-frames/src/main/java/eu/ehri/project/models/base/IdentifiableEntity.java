package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Mandatory;

public interface IdentifiableEntity extends Frame {

    @Mandatory
    @Property(Ontology.IDENTIFIER_KEY)
    public String getIdentifier();
}
