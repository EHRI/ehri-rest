package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Annotation {

    public static final String ANNOTATES = "annotates";

    @Adjacency(label = UserProfile.HAS_ANNOTATION, direction = Direction.IN)
    public UserProfile getUser();

    @Adjacency(label = ANNOTATES)
    public AccessibleEntity getTarget();

    @Property("body")
    public String getBody();

    @Property("body")
    public void setBody(String body);

    @Property("field")
    public String getField();

    @Property("field")
    public void setField(String field);

    @Property("startChar")
    public Long getStartChar();

    @Property("startChar")
    public void setStartChar(Long startChar);

    @Property("endChar")
    public Long getEndChar();

    @Property("endChar")
    public void setEndChar(Long endChar);
}
