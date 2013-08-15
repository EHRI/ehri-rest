package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Mandatory;

public interface NamedEntity extends Frame {

    @Mandatory
    @Property(Ontology.NAME_KEY)
    public String getName();
}
