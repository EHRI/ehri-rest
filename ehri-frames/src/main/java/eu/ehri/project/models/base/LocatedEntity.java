package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.GeoLocation;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface LocatedEntity extends Frame {
    @Dependent
    @Fetch(value = Ontology.HAS_GEO_LOCATION)
    @Adjacency(label = Ontology.HAS_GEO_LOCATION, direction = Direction.OUT)
    public Iterable<GeoLocation> getGeoLocations();
}
