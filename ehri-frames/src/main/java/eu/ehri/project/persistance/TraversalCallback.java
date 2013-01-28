package eu.ehri.project.persistance;

import com.tinkerpop.frames.VertexFrame;

public interface TraversalCallback {
    public void process(VertexFrame vertexFrame, int depth, String relation, int relationIndex);
}
