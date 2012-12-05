package eu.ehri.project.test.utils.fixtures.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.RuntimeErrorException;

import org.apache.commons.collections.map.MultiValueMap;
import org.hamcrest.core.IsInstanceOf;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.yaml.snakeyaml.Yaml;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;

public class YamlFixtureLoader implements FixtureLoader {

    public static final String DESCRIPTOR_KEY = "_desc";

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;

    public YamlFixtureLoader(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    private void loadFixtures() {
        loadFixtureFile(this.getClass().getClassLoader()
                .getResourceAsStream("initial.yaml"));
        loadFixtureFile(this.getClass().getClassLoader()
                .getResourceAsStream("testdata.yaml"));
    }

    private void loadFixtureFile(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        try {
            Map<Vertex, MultiValueMap> links = new HashMap<Vertex, MultiValueMap>();

            Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
            for (Object data : yaml.loadAll(yamlStream)) {
                for (Map<String, Object> node : (List<Map<String, Object>>) data) {
                    importNode(links, node);
                }
            }
            tx.success();

            // Finally, go through and wire up all the non-dependent
            // relationships
            // Relationships always go from source to target...
            System.out.println("LINKING... ");
            for (Node node : graph.getBaseGraph().getRawGraph().getAllNodes()) {
                System.out.println(" - node: " + node);
                for (String p : node.getPropertyKeys()) {
                    System.out.println(String.format("  - %-20s : %s", p,
                            node.getProperty(p)));
                }
            }
            System.out.println();
            for (Entry<Vertex, MultiValueMap> entry : links.entrySet()) {
                System.out.println("Setting links for: " + entry.getKey());
                Vertex src = entry.getKey();
                MultiValueMap rels = entry.getValue();
                for (Object relname : rels.keySet()) {
                    if (relname instanceof String) {
                        Collection<Object> targets = rels
                                .getCollection(relname);
                        for (Object target : targets) {
                            if (target instanceof String) {
                                System.out.println(" - Linking: " + target);
                                Vertex dst = manager.getVertex((String) target);
                                addRelationship(src, dst, (String) relname);
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading YAML fixture", e);
        }
    }

    private void addRelationship(Vertex src, Vertex dst, String relname) {
        boolean found = false;
        for (Vertex v : src.getVertices(Direction.OUT, relname)) {
            if (v == dst) {
                found = true;
                break;
            }
        }
        if (!found)
            graph.addEdge(null, src, dst, (String) relname);
    }

    /**
     * @param links
     * @param node
     * @throws DeserializationError
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private void importNode(Map<Vertex, MultiValueMap> links,
            Map<String, Object> node) throws DeserializationError,
            ValidationError, IntegrityError, ItemNotFound {
        String id = (String) node.get(Converter.ID_KEY);
        String type = (String) node.get(Converter.TYPE_KEY);
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node
                .get(Converter.DATA_KEY);
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> nodeRels = (Map<String, List<Object>>) node
                .get(Converter.REL_KEY);
        System.out.println(String.format("Item: %s %s %s", id, type, nodeData));

        MultiValueMap rels = getDependentRelations(nodeRels);
        Map<String, Object> dataBundle = new HashMap<String, Object>();
        dataBundle.put(Converter.ID_KEY, id);
        dataBundle.put(Converter.TYPE_KEY, type);
        dataBundle.put(Converter.DATA_KEY, nodeData);
        dataBundle.put(Converter.REL_KEY, rels);
        EntityBundle<VertexFrame> entityBundle = new Converter()
                .dataToBundle(dataBundle);
        BundleDAO<VertexFrame> persister = new BundleDAO<VertexFrame>(graph,
                SystemScope.getInstance());
        System.out.println("CREATING NODE: " + id);
        VertexFrame frame = persister.createOrUpdate(entityBundle);

        MultiValueMap linkRels = getLinkedRelations(nodeRels);
        if (!linkRels.isEmpty()) {
            links.put(frame.asVertex(), linkRels);
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

    /**
     * Extract from the relations the IDs of other non-dependent nodes.
     * 
     * @param data
     * @return
     */
    private MultiValueMap getLinkedRelations(Map<String, List<Object>> data) {
        MultiValueMap rels = new MultiValueMap();
        if (data != null) {
            for (Entry<String, List<Object>> entry : data.entrySet()) {
                String relName = entry.getKey();
                List<Object> relItems = entry.getValue();
                for (Object relation : relItems) {
                    if (relation instanceof String) {
                        rels.put(relName, relation);
                    }
                }
            }
        }
        return rels;
    }

    /**
     * Extract from the relations the nested dependent items.
     * 
     * @param data
     * @return
     */
    private MultiValueMap getDependentRelations(Map<String, List<Object>> data) {
        MultiValueMap rels = new MultiValueMap();
        if (data != null) {
            for (Entry<String, List<Object>> entry : data.entrySet()) {
                String relName = entry.getKey();
                List<Object> relItems = entry.getValue();
                for (Object relation : relItems) {
                    if (relation instanceof Map) {
                        rels.put(relName, relation);
                    }
                }
            }
        }
        return rels;
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
