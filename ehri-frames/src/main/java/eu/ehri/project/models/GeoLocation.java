package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.Frame;

/**
 * Type representing a geo-location.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.GEO_LOCATION)
public interface GeoLocation extends Frame {
    @Mandatory
    @Property(Ontology.LATITUDE)
    public Double getLatitude();

    @Mandatory
    @Property(Ontology.LONGITUDE)
    public Double getLongitude();
}
