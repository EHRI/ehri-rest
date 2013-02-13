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

import java.util.List;

/**
 * @author linda
 */
public class AbstractImporterTest extends AbstractFixtureTest {
    protected void printGraph(FramedGraph<Neo4jGraph> graph) {
        for (Vertex v : graph.getVertices()) {
            System.out.println("-------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof List) {
                    for (Object o : (List) v.getProperty(key)) {
                        value += o.toString() + " ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                System.out.println(key + ": " + value);
            }
//            
            for (Edge e : v.getEdges(Direction.OUT)) {
                System.out.println(e.getLabel());
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
