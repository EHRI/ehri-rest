package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Action;

public interface Actioner extends VertexFrame {
    
    public static final String HAS_ACTION = "hasAction";
    
    @Property("name")
    public String getName();
    
    @Adjacency(label=HAS_ACTION)
    public Iterable<Action> getActions();
    
}
