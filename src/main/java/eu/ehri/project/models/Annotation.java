package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.relationships.Annotates;

public interface Annotation extends AnnotatableEntity {

    public static final String isA = "annotation";
    public static final String ANNOTATES = "annotates";

    @Adjacency(label = UserProfile.HAS_ANNOTATION, direction = Direction.IN)
    public UserProfile getUser();

    @Adjacency(label = ANNOTATES)
    public AnnotatableEntity getTarget();
    
    @Incidence(label= ANNOTATES)
    public Annotates getContext();

    @Property("body")
    public String getBody();

    @Property("body")
    public void setBody(String body);

}
