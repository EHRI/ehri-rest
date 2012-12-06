package eu.ehri.project.test.utils.fixtures.impl;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.map.MultiValueMap;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.Adjacency;
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
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;

/**
 * Load data from YAML fixtures.
 * 
 * @author michaelb
 * 
 */
public class YamlFixtureLoader implements FixtureLoader {

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;
    private final Map<String, Class<? extends VertexFrame>> classes;
    private static final Logger logger = LoggerFactory
            .getLogger(YamlFixtureLoader.class);

    public YamlFixtureLoader(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
        classes = ClassUtils.getEntityClasses();
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

            for (Object data : yaml.loadAll(yamlStream)) {
                for (Map<String, Object> node : (List<Map<String, Object>>) data) {
                    logger.debug("Importing node: {}", node);
                    importNode(links, node);
                }
            }

            // Finally, go through and wire up all the non-dependent
            // relationships
            // Relationships always go from source to target...
            logger.debug("Linking data...");
            for (Entry<Vertex, MultiValueMap> entry : links.entrySet()) {
                logger.debug("Setting links for: {}", entry.getKey());
                Vertex src = entry.getKey();
                MultiValueMap rels = entry.getValue();
                for (Object relname : rels.keySet()) {
                    if (relname instanceof String) {
                        Collection<Object> targets = rels
                                .getCollection(relname);
                        for (Object target : targets) {
                            if (target instanceof String) {
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
        Direction direction = getDirectionOfRelationship(src, dst, relname);
        for (Vertex v : src.getVertices(direction, relname)) {
            if (v == dst) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (direction == Direction.OUT) {
                logger.debug(String.format(" - %s -[%s]-> %s", src, dst, relname));
                graph.addEdge(null, src, dst, (String) relname);                
            } else {
                logger.debug(String.format(" - %s -[%s]-> %s", dst, src, relname));
                graph.addEdge(null, dst, src, (String) relname);
            }
            graph.getBaseGraph().stopTransaction(
                    TransactionalGraph.Conclusion.SUCCESS);
        }
    }

    // Copied and pasted from BundleDAO for the moment...
    private Direction getDirectionOfRelationship(Vertex a, Vertex b, String rel) {
        Class<?> classA = getClassForVertex(a);
        Class<?> classB = getClassForVertex(b);
        for (Method method : classA.getMethods()) {
            Adjacency adj = method.getAnnotation(Adjacency.class);
            if (adj != null && adj.label().equals(rel)) {
                return adj.direction();
            }
        }
        for (Method method : classB.getMethods()) {
            Adjacency adj = method.getAnnotation(Adjacency.class);
            if (adj != null && adj.label().equals(rel)) {
                return adj.direction();
            }
        }
        // If we get here then something has gone badly wrong, because the
        // correct direction could not be found. Maybe it's better to just
        // ignore saving the dependency in the long run?
        throw new RuntimeException(
                String.format(
                        "Unable to find the direction of relationship between dependent classes with relationship '%s': '%s', '%s'",
                        rel, classA.getName(), classB.getName()));
    }

    private Class<?> getClassForVertex(Vertex a) {
        return classes.get((String) a.getProperty(EntityType.TYPE_KEY));
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
        if (nodeData == null)
            nodeData = new HashMap<String, Object>();
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeRels = (Map<String, Object>) node
                .get(Converter.REL_KEY);
        MultiValueMap rels = getDependentRelations(nodeRels);
        Map<String, Object> dataBundle = new HashMap<String, Object>();
        dataBundle.put(Converter.ID_KEY, id);
        dataBundle.put(Converter.TYPE_KEY, type);
        dataBundle.put(Converter.DATA_KEY, nodeData);
        dataBundle.put(Converter.REL_KEY, rels);
        Bundle<VertexFrame> entityBundle = new Converter()
                .dataToBundle(dataBundle);
        BundleDAO<VertexFrame> persister = new BundleDAO<VertexFrame>(graph,
                SystemScope.getInstance());
        logger.debug("Creating node with id: {}", id);
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
    private MultiValueMap getLinkedRelations(Map<String, Object> data) {
        MultiValueMap rels = new MultiValueMap();
        if (data != null) {
            for (Entry<String, Object> entry : data.entrySet()) {
                String relName = entry.getKey();
                Object relValue = entry.getValue();
                if (relValue instanceof List) {
                    for (Object relation : (List<?>) relValue) {
                        if (relation instanceof String) {
                            rels.put(relName, relation);
                        }
                    }
                } else if (relValue instanceof String) {
                    rels.put(relName, relValue);
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
    private MultiValueMap getDependentRelations(Map<String, Object> data) {
        MultiValueMap rels = new MultiValueMap();
        if (data != null) {
            for (Entry<String, Object> entry : data.entrySet()) {
                String relName = entry.getKey();
                Object relValue = entry.getValue();
                if (relValue instanceof List) {
                    for (Object relation : (List<?>) relValue) {
                        if (relation instanceof Map) {
                            rels.put(relName, relation);
                        }
                    }
                } else if (relValue instanceof Map) {
                    rels.put(relName, relValue);
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
