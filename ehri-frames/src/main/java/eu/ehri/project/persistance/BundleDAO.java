package eu.ehri.project.persistance;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.map.MultiValueMap;
import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.DocumentaryUnitIdGenerator;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class responsible for persisting and deleting an EntityBundle<T>, a data
 * structure representing a graph node and its relations to be updated in a
 * single batch.
 * 
 * @param <T>
 */
public final class BundleDAO {

    private final FramedGraph<Neo4jGraph> graph;
    private final PermissionScope scope;
    private final GraphManager manager;

    /**
     * Constructor with a given scope.
     * 
     * @param graph
     * @param scope
     */
    public BundleDAO(FramedGraph<Neo4jGraph> graph, PermissionScope scope) {
        this.graph = graph;
        this.scope = scope;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor with system scope.
     * 
     * @param graph
     */
    public BundleDAO(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Entry-point for updating a bundle.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    public <T extends VertexFrame> T update(Bundle bundle, Class<T> cls)
            throws ValidationError, IntegrityError, ItemNotFound {
        return graph.frame(updateInner(bundle), cls);
    }

    /**
     * Entry-point for creating a bundle.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     */
    public <T extends VertexFrame> T create(Bundle bundle, Class<T> cls)
            throws ValidationError, IntegrityError {
        return graph.frame(createInner(bundle), cls);
    }

    /**
     * Entry point for creating or updating a bundle, depending on whether it
     * has a supplied id.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     */
    public <T extends VertexFrame> T createOrUpdate(Bundle bundle, Class<T> cls)
            throws ValidationError, IntegrityError {
        return graph.frame(createOrUpdateInner(bundle), cls);
    }

    /**
     * Delete a bundle and dependent items, returning the total number of
     * vertices deleted.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     */
    public Integer delete(Bundle bundle) throws ValidationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Integer count = deleteCount(bundle, 0);
            tx.success();
            return count;
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }

    // Helpers

    private Integer deleteCount(Bundle bundle, Integer count)
            throws ValidationError, ItemNotFound {
        Integer c = count;
        MultiValueMap fetch = bundle.getRelations();
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(bundle.getBundleClass());
        for (Object key : fetch.keySet()) {
            for (Object obj : fetch.getCollection(key)) {
                // FIXME: Make it so we don't typically do this check for
                // Dependent relations
                if (dependents.containsKey(key)) {
                    Bundle sub = (Bundle) obj;
                    c = deleteCount(sub, c);
                }
            }
        }
        manager.deleteVertex(bundle.getId());
        c += 1;
        return c;
    }

    /**
     * Insert or update an item depending on a) whether it has an ID, and b)
     * whether it has an ID and already exists. If import mode is not enabled an
     * error will be thrown.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private Vertex createOrUpdateInner(Bundle bundle) throws ValidationError,
            IntegrityError {
        if (bundle.getId() == null) {
            return createInner(bundle);
        } else {
            try {
                return manager.exists(bundle.getId()) ? updateInner(bundle)
                        : createInner(bundle);
            } catch (ItemNotFound e) {
                throw new RuntimeException(
                        "Create or update failed because ItemNotFound was thrown even though exists() was true",
                        e);
            }
        }
    }

    /**
     * Insert a bundle and save it's dependent items.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private Vertex createInner(Bundle bundle) throws ValidationError,
            IntegrityError {
        try {
            BundleValidatorFactory.getInstance(bundle).validate();
            // If the bundle doesn't already have an ID, generate one using the
            // (presently stopgap) type-dependent ID generator.
            String id = bundle.getId() != null ? bundle.getId()
                    : getIdGenerator(bundle).generateId(bundle.getType(),
                            scope, bundle.getData());
            Vertex node = manager.createVertex(id, bundle.getType(),
                    bundle.getData(), bundle.getPropertyKeys(),
                    bundle.getUniquePropertyKeys());
            createDependents(node, bundle.getBundleClass(),
                    bundle.getRelations());
            return node;
        } catch (IdGenerationError err) {
            throw new RuntimeException(err.getMessage());
        }
    }

    /**
     * Update a bundle and save its dependent items.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private Vertex updateInner(Bundle bundle) throws ValidationError,
            IntegrityError, ItemNotFound {
        BundleValidatorFactory.getInstance(bundle).validateForUpdate();
        Vertex node = manager.updateVertex(bundle.getId(), bundle.getType(),
                bundle.getData(), bundle.getPropertyKeys(),
                bundle.getUniquePropertyKeys());
        updateDependents(node, bundle.getBundleClass(), bundle.getRelations());
        return node;
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are
     * not dependent are ignored.
     * 
     * @param master
     * @param cls
     * @param relations
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private void createDependents(Vertex master, Class<?> cls,
            MultiValueMap relations) throws ValidationError, IntegrityError {
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(cls);
        for (Object key : relations.keySet()) {
            String relation = (String) key;
            if (dependents.containsKey(relation)) {

                for (Object obj : relations.getCollection(key)) {
                    Bundle bundle = (Bundle) obj;
                    Vertex child = createInner(bundle);
                    createChildRelationship(master, child, relation,
                            dependents.get(relation));
                }
            }
        }
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are
     * not dependent are ignored.
     * 
     * @param master
     * @param cls
     * @param relations
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private void updateDependents(Vertex master, Class<?> cls,
            MultiValueMap relations) throws ValidationError, IntegrityError,
            ItemNotFound {

        // Get a list of dependent relationships for this class, and their
        // directions.
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(cls);
        // Build a list of the IDs of existing dependents we're going to be
        // updating.
        Set<String> updating = getUpdateSet(relations);
        // Any that we're not going to update can have their subtrees deleted.
        deleteMissingFromUpdateSet(master, dependents, updating);

        // Now go throw and create or update the new subtrees.
        for (Object key : relations.keySet()) {
            String relation = (String) key;
            if (dependents.containsKey(relation)) {

                for (Object obj : relations.getCollection(key)) {
                    Bundle bundle = (Bundle) obj;
                    Vertex child = createOrUpdateInner(bundle);
                    Direction direction = dependents.get(relation);

                    // FIXME: Traversing all the current relations here (for
                    // every individual dependent) is very inefficient!
                    HashSet<Vertex> currentRels = getCurrentRelationships(
                            master, direction, relation);

                    // Create a relation if there isn't one already
                    if (!currentRels.contains(child)) {
                        createChildRelationship(master, child, relation,
                                direction);
                    }
                }
            }
        }
    }

    private Set<String> getUpdateSet(MultiValueMap relations) {
        Set<String> updating = new HashSet<String>();
        for (Object relation : relations.keySet()) {
            for (Object child : relations.getCollection(relation)) {
                if (child instanceof Bundle) {
                    updating.add(((Bundle) child).getId());
                }
            }
        }
        return updating;
    }

    private void deleteMissingFromUpdateSet(Vertex master,
            Map<String, Direction> dependents, Set<String> updating)
            throws ValidationError {
        Converter converter = new Converter();
        for (Entry<String, Direction> relEntry : dependents.entrySet()) {
            for (Vertex v : getCurrentRelationships(master,
                    relEntry.getValue(), relEntry.getKey())) {
                if (!updating.contains(v.getProperty(EntityType.ID_KEY))) {
                    try {
                        delete(converter.vertexFrameToBundle(graph.frame(v,
                                VertexFrame.class)));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Get the IDs of nodes that terminate a given relationship from a
     * particular source node.
     * 
     * @param src
     * @param direction
     * @param label
     * @return
     */
    private HashSet<Vertex> getCurrentRelationships(final Vertex src,
            Direction direction, String label) {
        HashSet<Vertex> out = new HashSet<Vertex>();
        for (Vertex end : src.getVertices(direction, label)) {
            out.add(end);
        }
        return out;
    }

    /**
     * Create a
     * 
     * @param master
     * @param child
     * @param label
     * @param direction
     */
    private void createChildRelationship(Vertex master, Vertex child,
            String label, Direction direction) {
        if (direction == Direction.OUT) {
            graph.addEdge(null, master, child, label);
        } else {
            graph.addEdge(null, child, master, label);
        }
    }

    // FIXME: Put this logic somewhere else. When EntityTypes is converted
    // to an Enum we'll be able to associate an ID generator with a
    // particular type, but for now do it the crappy hacky way.
    private IdGenerator getIdGenerator(Bundle bundle) {
        if (DocumentaryUnit.class.isAssignableFrom(bundle.getBundleClass())) {
            return new DocumentaryUnitIdGenerator();
        } else if (AccessibleEntity.class.isAssignableFrom(bundle
                .getBundleClass())) {
            return new AccessibleEntityIdGenerator();
        } else {
            return new GenericIdGenerator();
        }
    }
}
