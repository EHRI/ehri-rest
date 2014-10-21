package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.NamedEntity;

/**
 * Holds the information on a relationship specified in some Description,
 * but without the target-end of the relationship being determined.
 * 
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
@EntityType(EntityClass.UNDETERMINED_RELATIONSHIP)
public interface UndeterminedRelationship extends AccessibleEntity, NamedEntity, Annotator {

    /**
     * Fetch the description to which this UR belongs.
     *
     * @return a description frame
     */
    @Adjacency(label = Ontology.HAS_ACCESS_POINT, direction = Direction.IN)
    public Description getDescription();

    /**
     * Fetch the links which make up the body of this UR (if any.)
     *
     * @return an iterable of link frames
     */
    @Adjacency(label = Ontology.LINK_HAS_BODY, direction = Direction.IN)
    public Iterable<Link> getLinks();

    /**
     * Get the relationship type of this UR.
     *
     * @return A type string
     */
    @Mandatory
    @Property(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)
    public String getRelationshipType();
}




