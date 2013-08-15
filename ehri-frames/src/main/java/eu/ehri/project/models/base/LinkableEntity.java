package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Link;

/**
 * An entity that can hold incoming links.
 */
public interface LinkableEntity extends AccessibleEntity {
    @Adjacency(label = Ontology.LINK_HAS_TARGET, direction = Direction.IN)
    public Iterable<Link> getLinks();

    @Adjacency(label = Ontology.LINK_HAS_TARGET, direction = Direction.IN)
    public void addLink(final Link link);
}
