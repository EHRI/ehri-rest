package eu.ehri.project.core;

import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.impl.SingleIndexGraphManager;

public class GraphManagerFactory {
    public static GraphManager getInstance(FramedGraph<?> graph) {
        return new SingleIndexGraphManager(graph);
    }
}
