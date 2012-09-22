package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.relationships.Annotates;

@EntityType(EntityTypes.ANNOTATION)
public interface Annotation extends AnnotatableEntity, AccessibleEntity {

    public static final String ANNOTATES = "annotates";

    @Adjacency(label = Annotator.HAS_ANNOTATION, direction = Direction.IN)
    public Annotator getAnnotator();

    @Adjacency(label = ANNOTATES)
    public AnnotatableEntity getTarget();

    @Incidence(label = ANNOTATES, direction = Direction.OUT)
    public Iterable<Annotates> getContext();

    @Property("body")
    public String getBody();

    @Property("body")
    public void setBody(String body);

}
