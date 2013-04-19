/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Annotator;

/**
 *
 * Holds the information on a relationship specified in some Description,
 * but without the target-end of the relationship being determined.
 * 
 * @author linda
 */
@EntityType(EntityClass.UNDETERMINED_RELATIONSHIP)
public interface UndeterminedRelationship extends AccessibleEntity, Annotator {
    @Adjacency(label = Annotation.HAS_SOURCE, direction = Direction.IN)
    public Iterable<Annotation> getLinkedAnnotations();
}




