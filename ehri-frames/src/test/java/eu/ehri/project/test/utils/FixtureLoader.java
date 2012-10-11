package eu.ehri.project.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.models.annotations.EntityType;

public class FixtureLoader {

    public static final String DESCRIPTOR_KEY = "_desc";

    private FramedGraph<Neo4jGraph> graph;
    private GraphHelpers helpers;

    public FixtureLoader(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        helpers = new GraphHelpers(graph.getBaseGraph().getRawGraph());
    }

    public Vertex getTestVertex(String descriptor) {
        return graph.getBaseGraph().getVertices(DESCRIPTOR_KEY, descriptor)
                .iterator().next();
    }

    public <T> T getTestFrame(String descriptor, Class<T> cls) {
        return graph.getVertices(DESCRIPTOR_KEY, descriptor, cls).iterator()
                .next();
    }

    public Iterable<Vertex> getTestVertices(String entityType) {
        return graph.getBaseGraph().getVertices(EntityType.KEY, entityType);
    }

    public <T> Iterable<T> getTestFrames(String entityType, Class<T> cls) {
        return graph.getVertices(EntityType.KEY, entityType, cls);
    }

    private void loadNodes() {
        InputStream jsonStream = this.getClass().getClassLoader()
                .getResourceAsStream("vertices.json");
        try {
            List<Map<String, Map<String, Object>>> nodes = new ObjectMapper()
                    .readValue(jsonStream, List.class);

            for (Map<String, Map<String, Object>> namedNode : nodes) {
                Map<String, Object> data = namedNode.get("data");
                data.put(DESCRIPTOR_KEY, namedNode.get("desc"));
                String isa = (String) data.get(EntityType.KEY);

                Index<Vertex> index = helpers.getOrCreateIndex(isa,
                        Vertex.class);
                helpers.createIndexedVertex(data, index);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading JSON fixture", e);
        }
    }

    private void loadEdges() {
        InputStream jsonStream = this.getClass().getClassLoader()
                .getResourceAsStream("edges.json");
        try {
            List<Map<String, Object>> edges = new ObjectMapper().readValue(
                    jsonStream, List.class);
            for (Map<String, Object> edge : edges) {
                String srcdesc = (String) edge.get("src");
                String label = (String) edge.get("label");
                String dstdesc = (String) edge.get("dst");
                Map<String, Object> data = (Map<String, Object>) edge
                        .get("data");

                Index<Edge> index = helpers.getOrCreateIndex(label, Edge.class);
                helpers.createIndexedEdge(getTestVertex(srcdesc),
                        getTestVertex(dstdesc), label, data, index);
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
        } finally {
            tx.finish();
        }
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("No Neo4j database specified.");
        }

        GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabase(args[0]);
        registerShutdownHook(db);
        try {
            FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                    new Neo4jGraph(db));
            FixtureLoader loader = new FixtureLoader(graph);
            loader.loadTestData();
        } catch (Error e) {
            db.shutdown();
            throw new RuntimeErrorException(e);
        }
        System.exit(0);
    }
}
