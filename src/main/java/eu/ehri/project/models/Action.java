package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

public interface Action extends VertexFrame {
    public static final String HAS_SUBJECT = "hasSubject";

    @Adjacency(label=HAS_SUBJECT)
    public Iterable<VertexFrame> getSubjects();
}
