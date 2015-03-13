package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.Promotable;

/**
 * A frame class representing an annotation.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.ANNOTATION)
public interface Annotation extends AnnotatableEntity, AccessibleEntity, Promotable {

    /**
     * Fetch annotations that have been made <i>on this annotation.</i>
     *
     * @return an iterable of annotation frames
     */
    @Fetch(Ontology.ANNOTATION_ANNOTATES)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES, direction = Direction.IN)
    public Iterable<Annotation> getAnnotations();

    /**
     * Fetch the annotator of this annotation.
     *
     * @return an annotator frame
     */
    @Fetch(value = Ontology.ANNOTATOR_HAS_ANNOTATION, numLevels = 0)
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION, direction = Direction.IN)
    public Annotator getAnnotator();

    /**
     * Set the annotator of this annotation.
     *
     * @param annotator an annotator frame
     */
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION, direction = Direction.IN)
    public void setAnnotator(final Annotator annotator);

    /**
     * Fetch all targets of this annotation, including sub-parts
     * of an item.
     *
     * @return an iterable of annotatable items
     */
    @Fetch(value = Ontology.ANNOTATES_PART, numLevels = 1)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES_PART)
    public Iterable<AnnotatableEntity> getTargetParts();

    /**
     * Fetch the targets of this annotation.
     *
     * @return an iterable of annotatable items
     */
    @Fetch(value = Ontology.ANNOTATES, numLevels = 1)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES)
    public Iterable<AnnotatableEntity> getTargets();

    /**
     * Get the body of this annotation.
     *
     * @return the body text
     */
    @Mandatory
    @Property(Ontology.ANNOTATION_NOTES_BODY)
    public String getBody();
}
