package eu.ehri.project.core;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.Neo4jGraphManager;

public class GraphManagerFactory {
    public static GraphManager getInstance(FramedGraph<?> graph) {
        return new Neo4jGraphManager(graph);
        //return new BasicGraphManager(graph);
    }
}
