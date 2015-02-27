package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.LinkableEntity;

/**
 * An item that belongs in some authoritative set.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public interface AuthoritativeItem extends AccessibleEntity, LinkableEntity, IdentifiableEntity {
    @Fetch(Ontology.ITEM_IN_AUTHORITATIVE_SET)
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public AuthoritativeSet getAuthoritativeSet();
}
