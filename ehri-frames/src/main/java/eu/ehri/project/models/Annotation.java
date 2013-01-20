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

    public static final String ANNOTATES = "annotates";

    @Fetch
    @Adjacency(label = Annotation.ANNOTATES, direction = Direction.IN)
    public Iterable<Annotation> getAnnotations();

    @Fetch(depth = 1)
    @Adjacency(label = Annotator.HAS_ANNOTATION, direction = Direction.IN)
    public Annotator getAnnotator();

    @Adjacency(label = Annotator.HAS_ANNOTATION, direction = Direction.IN)
    public Annotator setAnnotator(final Annotator annotator);

    @Adjacency(label = ANNOTATES)
    public AnnotatableEntity getTarget();

    @Property("body")
    public String getBody();
}
