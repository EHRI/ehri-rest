/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.NamedEntity;

/**
 *
 * Holds the information on a relationship specified in some Description,
 * but without the target-end of the relationship being determined.
 * 
 * @author linda
 */
@EntityType(EntityClass.UNDETERMINED_RELATIONSHIP)
public interface UndeterminedRelationship extends AccessibleEntity, NamedEntity, Annotator {

    public static final String RELATIONSHIP_TYPE = "type";
    @Adjacency(label = Link.HAS_LINK_BODY, direction = Direction.IN)
    public Iterable<Link> getLinks();

    @Mandatory
    @Property(RELATIONSHIP_TYPE)
    public String getRelationshipType();
}




