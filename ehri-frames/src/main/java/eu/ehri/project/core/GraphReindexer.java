package eu.ehri.project.core;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;

/**
 * Reindex the internal graph index.
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 */
public class GraphReindexer {

    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;

    public GraphReindexer(FramedGraph<? extends TransactionalGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * recreate the index for all the Entity vertices
     */
    public void reindex() {
        // clear the index
        try {
            manager.rebuildIndex();
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
        }
    }
}