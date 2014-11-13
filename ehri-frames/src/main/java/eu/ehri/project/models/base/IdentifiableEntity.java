package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Mandatory;

/**
 * Base interface for entities that have an identifier property (other than the
 * internally assigned node ID).
 *
 * @author Mike Bryant (https://github.com/mikesname)
 *
 */
public interface IdentifiableEntity extends Frame {

    @Mandatory
    @Property(Ontology.IDENTIFIER_KEY)
    public String getIdentifier();
}
