package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Action extends VertexFrame {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_ACTIONER = "hasActioner";

    @Property("timestamp")
    public String getTimestamp();
    
    @Property("timestamp")
    public void setTimestamp(String timestamp);
    
    @Property("logMessage")
    public String getLogMessage();
    
    @Property("logMessage")
    public void setLogMessage(String message);    
    
    @Adjacency(label = HAS_SUBJECT)
    public Iterable<VertexFrame> getSubjects();
    
    @Adjacency(label = HAS_SUBJECT)
    public void addSubjects(final VertexFrame subject);
    
    @Adjacency(label = HAS_SUBJECT)
    public void setSubject(final VertexFrame subject);
    
    @Adjacency(label = HAS_ACTIONER)
    public UserProfile getActioner();
    
    @Adjacency(label = HAS_ACTIONER)
    public void setActioner(final UserProfile user);
}
