package eu.ehri.extension.base;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;

/**
 * A resource which handles a transactional graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface TxCheckedResource {
    /**
     * Fetch the graph associated with the resource.
     *
     * @return a transactional graph database.
     */
    public FramedGraph<TxCheckedNeo4jGraph> getGraph();
}
