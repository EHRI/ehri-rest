package eu.ehri.project.test.utils.fixtures.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.LinkedListMultimap;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;

public class JsonFixtureLoader implements FixtureLoader {

    public static final String DESCRIPTOR_KEY = EntityType.ID_KEY;

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;

    public JsonFixtureLoader(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    @SuppressWarnings("unchecked")
    private void loadNodes() {
        InputStream jsonStream = this.getClass().getClassLoader()
                .getResourceAsStream("vertices.json");

        try {
            List<Map<String, Object>> nodes = new ObjectMapper().readValue(
                    jsonStream, List.class);

            for (Map<String, Object> namedNode : nodes) {
                String id = (String) namedNode.get(EntityType.ID_KEY);
                EntityClass isa = EntityClass.withName((String) namedNode.get(EntityType.TYPE_KEY));
                Map<String, Object> data = (Map<String, Object>) namedNode
                        .get("data");
                Bundle bundle = new Bundle(id, isa, data, LinkedListMultimap.<String,Bundle>create());
                manager.createVertex(id, bundle.getType(), bundle.getData(),
                        bundle.getPropertyKeys(),
                        bundle.getUniquePropertyKeys());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading JSON fixture", e);
        }
    }

    private void loadEdges() {
        InputStream jsonStream = this.getClass().getClassLoader()
                .getResourceAsStream("edges.json");
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> edges = new ObjectMapper().readValue(
                    jsonStream, List.class);
            for (Map<String, Object> edge : edges) {
                String srcdesc = (String) edge.get("src");
                String label = (String) edge.get("label");
                String dstdesc = (String) edge.get("dst");
                graph.addEdge(null, getTestVertex(srcdesc),
                        getTestVertex(dstdesc), label);
                // FIXME: Should we commit here? It seems to give a
                // "org.neo4j.graphdb.TransactionFailureException: Failed to mark transaction as rollback only."
                // exception otherwise.
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.SUCCESS);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading JSON fixture", e);
        } finally {
            try {
                jsonStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void loadTestData() {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            loadNodes();
            loadEdges();
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }

    // Helpers
    private Vertex getTestVertex(String descriptor) {
        return graph.getBaseGraph().getVertices(DESCRIPTOR_KEY, descriptor)
                .iterator().next();
    }
}
