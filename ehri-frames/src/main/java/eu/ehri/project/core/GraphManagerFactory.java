package eu.ehri.project.core;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.impl.SingleIndexGraphManager;

public class GraphManagerFactory {
    public static GraphManager getInstance(FramedGraph<Neo4jGraph> graph) {
        return new SingleIndexGraphManager(graph);
    }
}
