package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public interface AuthoritativeItem extends AccessibleEntity {
    @Fetch(Ontology.ITEM_IN_AUTHORITATIVE_SET)
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public AuthoritativeSet getAuthoritativeSet();
}
