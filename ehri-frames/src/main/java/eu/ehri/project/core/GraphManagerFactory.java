package eu.ehri.project.core;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.BlueprintsGraphManager;
import eu.ehri.project.core.impl.Neo4jGraphManager;

public class GraphManagerFactory {

    /**
     * Obtain an instance of a graph manager.
     *
     * @param graph An indexable and transactional Blueprints graph.
     * @return A graph manager instance.
     */
    // NB: Because Java doesn't support multiple wildcard bounds we do some checking
    // of the bounds manually ourselves, which is ugly but should ensure it's safe
    // to do an unchecked cast here.
    @SuppressWarnings("unchecked")
    public static GraphManager getInstance(FramedGraph<?> graph) {
        Graph baseGraph = graph.getBaseGraph();

        if (!IndexableGraph.class.isAssignableFrom(baseGraph.getClass())) {
            throw new RuntimeException("Graph instance must be transactional and indexable");
        }

        if (Neo4jGraph.class.isAssignableFrom(baseGraph.getClass())) {
            return new Neo4jGraphManager(graph);
        } else {
            return new BlueprintsGraphManager(graph);
        }
    }
}
