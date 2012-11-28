package eu.ehri.project.test.utils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.yaml.snakeyaml.Yaml;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.models.annotations.EntityType;

public class YamlFixtureLoader {

    public static final String DESCRIPTOR_KEY = "_desc";

    private FramedGraph<Neo4jGraph> graph;
    private GraphHelpers helpers;

    public YamlFixtureLoader(FramedGraph<Neo4jGraph> graph) {
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
        return graph.getBaseGraph().getVertices(EntityType.TYPE_KEY, entityType);
    }

    public <T> Iterable<T> getTestFrames(String entityType, Class<T> cls) {
        return graph.getVertices(EntityType.TYPE_KEY, entityType, cls);
    }

    private void loadFixtures() {
        InputStream yamlStream = this.getClass().getClassLoader()
                .getResourceAsStream("test-graph.yaml");
        Yaml yaml = new Yaml();
        try {
            
            for (Object data : yaml.loadAll(yamlStream)) {
                for (Entry<String,Object> entry : ((Map<String,Object>)data).entrySet()) {
                    System.out.println("TYPE: " + entry.getKey());
                    List<Map<String,Object>> items = (List<Map<String,Object>>)entry.getValue();
                    for (Map<String,Object> itemSets: items) {
                        for (Entry<String,Object> item : itemSets.entrySet()) {
                            System.out.println(" - ITEM: " + item.getKey() + ": " + item.getValue());
                        }
                    }
                }
            }            
        } catch (Exception e) {
            throw new RuntimeException("Error loading JSON fixture", e);
        }
    }

    public void loadTestData() {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            loadFixtures();
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
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
            YamlFixtureLoader loader = new YamlFixtureLoader(graph);
            loader.loadTestData();
        } catch (Error e) {
            db.shutdown();
            throw new RuntimeException(e);
        }
        System.exit(0);
    }
}
