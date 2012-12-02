package eu.ehri.project.test.utils.fixtures.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.yaml.snakeyaml.Yaml;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;

public class YamlFixtureLoader implements FixtureLoader {

    public static final String DESCRIPTOR_KEY = "_desc";

    private FramedGraph<Neo4jGraph> graph;
    private GraphManager manager;

    public YamlFixtureLoader(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    private void loadFixtures() {
        InputStream yamlStream = this.getClass().getClassLoader()
                .getResourceAsStream("test-graph.yaml");
        Yaml yaml = new Yaml();
        try {
            
            for (Object data : yaml.loadAll(yamlStream)) {
                for (Entry<String,Object> entry : ((Map<String,Object>)data).entrySet()) {
                    List<Map<String,Object>> items = (List<Map<String,Object>>)entry.getValue();
                    for (Map<String,Object> itemSets: items) {
                        for (Entry<String,Object> item : itemSets.entrySet()) {
                            String id = item.getKey();
                            String type = entry.getKey();
                            Map<String,Object> itemData = (Map<String, Object>) item.getValue();
                            System.out.println(String.format("Item: %s %s %s", id, type, itemData));
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
