package eu.ehri.project.models;

import com.tinkerpop.frames.*;

public interface Annotation {

    @Adjacency(label="hasAnnotation") public UserProfile getUser();
    @Adjacency(label="annotates") public Entity getTarget();

    @Property("body") public String getBody();
    @Property("body") public void setBody(String body);
    @Property("field") public String getField();
    @Property("field") public void setField(String field);
    @Property("startChar") public Long getStartChar();
    @Property("startChar") public void setStartChar(Long startChar);
    @Property("endChar") public Long getEndChar();
    @Property("endChar") public void setEndChar(Long endChar);
}


