package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.Annotator;

@EntityType(EntityClass.ANNOTATION)
public interface Annotation extends AnnotatableEntity, AccessibleEntity {

    public static final String ANNOTATES = "hasAnnotationTarget";
    
    public static final String HAS_SOURCE = "hasAnnotationBody";
    public static final String NOTES_BODY = "body";
    public static final String ANNOTATION_TYPE = "type";

    @Fetch(Annotation.ANNOTATES)
    @Adjacency(label = Annotation.ANNOTATES, direction = Direction.IN)
    public Iterable<Annotation> getAnnotations();

    @Fetch(value = Annotator.HAS_ANNOTATION, depth = 1)
    @Adjacency(label = Annotator.HAS_ANNOTATION, direction = Direction.IN)
    public Annotator getAnnotator();

    @Adjacency(label = Annotator.HAS_ANNOTATION, direction = Direction.IN)
    public Annotator setAnnotator(final Annotator annotator);

    @Adjacency(label = ANNOTATES)
    public Iterable<AnnotatableEntity> getTargets();

    @Adjacency(label = HAS_SOURCE)
    public void addSource(final Annotator annotator);

    @Property(NOTES_BODY)
    public String getBody();
}
