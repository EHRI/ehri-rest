package eu.ehri.extension;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;

/**
 * Created by mike on 18/06/13.
 */
public interface TxCheckedResource {
    public FramedGraph<TxCheckedNeo4jGraph> getGraph();
}
