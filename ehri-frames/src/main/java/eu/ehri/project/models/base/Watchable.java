package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.UserProfile;

import static eu.ehri.project.definitions.Ontology.USER_WATCHING_ITEM;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Watchable extends Frame {
    @Adjacency(label = USER_WATCHING_ITEM, direction = Direction.IN)
    public Iterable<UserProfile> getWatchers();
}
