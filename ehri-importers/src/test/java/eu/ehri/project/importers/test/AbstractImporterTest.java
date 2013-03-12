/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.test;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.test.AbstractFixtureTest;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author linda
 */
public class AbstractImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractImporterTest.class);
    protected void printGraph(FramedGraph<Neo4jGraph> graph) {
        for (Vertex v : graph.getVertices()) {
            logger.debug("-------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = (String[]) v.getProperty(key);
                    for (String o : list) {
                        value += "["+o.toString() + "] ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                logger.debug(key + ": " + value);
            }
//            
            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

    protected Vertex getVertexByIdentifier(FramedGraph<Neo4jGraph> graph, String id) {
        Iterable<Vertex> docs = graph.getVertices(AccessibleEntity.IDENTIFIER_KEY, id);
        return docs.iterator().next();
    }

    protected int getNodeCount(FramedGraph<Neo4jGraph> graph) {
        return toList(GlobalGraphOperations
                .at(graph.getBaseGraph().getRawGraph()).getAllNodes()).size();
    }
}
