package eu.ehri.project.relationships;

import com.tinkerpop.frames.Domain;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.Range;

import eu.ehri.project.models.AnnotatableEntity;
import eu.ehri.project.models.UserProfile;

public interface Annotates {

    @Range
    public AnnotatableEntity getEntity();

    @Domain
    public UserProfile getAnnotator();
    
    @Property("timestamp")
    public String getTimestamp();
    
    @Property("startChar")
    public Long getStartChar();
    
    @Property("startChar")
    public void setStartChar(Long startChar);
    
    @Property("endChar")
    public Long getEndChar();
    
    @Property("endChar")
    public void setEndChar(Long endChar);
    
    @Property("field")
    public String getField();
    
    @Property("field")
    public void setField(String field);
}
