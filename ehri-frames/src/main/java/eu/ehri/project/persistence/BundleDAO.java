package eu.ehri.project.persistence;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class responsible for creating, updating and deleting Bundles.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class BundleDAO {

    private static final Logger logger = LoggerFactory.getLogger(BundleDAO.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Serializer serializer;
    private final BundleValidator validator;

    /**
     * Constructor with a given scope.
     *
     * @param graph
     * @param scopeIds
     */
    public BundleDAO(FramedGraph<?> graph, Iterable<String> scopeIds) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).dependentOnly().build();
        validator = new BundleValidator(manager, scopeIds);
    }

    /**
     * Constructor with system scope.
     *
     * @param graph
     */
    public BundleDAO(FramedGraph<?> graph) {
        this(graph, Lists.<String>newArrayList());
    }

    /**
     * Entry-point for updating a bundle.
     *
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public <T extends Frame> Mutation<T> update(Bundle bundle, Class<T> cls)
            throws ValidationError, ItemNotFound {
        Bundle bundleWithIds = validator.validateForUpdate(bundle);
        Mutation<Vertex> mutation = updateInner(bundleWithIds);
        return new Mutation<T>(graph.frame(mutation.getNode(), cls),
                mutation.getState(), mutation.getPrior());
    }

    /**
     * Entry-point for creating a bundle.
     *
     * @param bundle
     * @return
     * @throws ValidationError
     */
    public <T extends Frame> T create(Bundle bundle, Class<T> cls)
            throws ValidationError {
        Bundle bundleWithIds = validator.validateForCreate(bundle);
        return graph.frame(createInner(bundleWithIds), cls);
    }

    /**
     * Entry point for creating or updating a bundle, depending on whether it has a supplied id.
     *
     * @param bundle
     * @return
     * @throws ValidationError
     */
    public <T extends Frame> Mutation<T> createOrUpdate(Bundle bundle, Class<T> cls)
            throws ValidationError {
        Bundle bundleWithIds = validator.validateForUpdate(bundle);
        Mutation<Vertex> vertexMutation = createOrUpdateInner(bundleWithIds);
        return new Mutation<T>(graph.frame(vertexMutation.getNode(), cls), vertexMutation.getState(),
                vertexMutation.getPrior());
    }

    /**
     * Delete a bundle and dependent items, returning the total number of vertices deleted.
     *
     * @param bundle
     * @return
     */
    public Integer delete(Bundle bundle) {
        try {
            return deleteCount(bundle, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helpers
    private Integer deleteCount(Bundle bundle, int count) throws Exception {
        Integer c = count;

        for (Bundle child : bundle.getDependentRelations().values()) {
            c = deleteCount(child, c);
        }
        manager.deleteVertex(bundle.getId());
        return c + 1;
    }

    /**
     * Insert or update an item depending on a) whether it has an ID, and b) whether it has an ID and already exists. If
     * import mode is not enabled an error will be thrown.
     *
     * @param bundle
     * @return
     * @throws RuntimeException when an item is said to exist, but could not be found
     */
    private Mutation<Vertex> createOrUpdateInner(Bundle bundle) {
        try {
            if (manager.exists(bundle.getId())) {
                return updateInner(bundle);
            } else {
                return new Mutation<Vertex>(createInner(bundle), MutationState.CREATED);
            }
        } catch (ItemNotFound e) {
            throw new RuntimeException(
                    "Create or update failed because ItemNotFound was thrown even though exists() was true",
                    e);
        }
    }

    /**
     * Insert a bundle and save its dependent items.
     *
     * @param bundle a Bundle to insert in the graph
     * @return the Vertex that was created from this Bundle
     * @throws RuntimeException when an ID generation error was not handled by the IdGenerator class
     */
    private Vertex createInner(Bundle bundle) {
        try {
            Vertex node = manager.createVertex(bundle.getId(), bundle.getType(),
                    bundle.getData(), bundle.getPropertyKeys());
            createDependents(node, bundle.getBundleClass(), bundle.getRelations());
            return node;
        } catch (IntegrityError e) {
            // Mmmn, if we get here, it means that there's been an ID generation error
            // which was not handled by an exception.. so throw a runtime error...
            throw new RuntimeException(
                    "Unexpected state: ID generation error not handled by IdGenerator class");
        }
    }

    /**
     * Update a bundle and save its dependent items.
     *
     * @param bundle
     * @return
     * @throws ItemNotFound
     */
    private Mutation<Vertex> updateInner(Bundle bundle) throws ItemNotFound {
        Vertex node = manager.getVertex(bundle.getId());
        try {
            Bundle nodeBundle = serializer.vertexFrameToBundle(node);
            if (!nodeBundle.equals(bundle)) {
                logger.trace("Bundles differ\n\n{}\n\n{}", bundle.toJson(), nodeBundle.toJson());
                node = manager.updateVertex(bundle.getId(), bundle.getType(),
                        bundle.getData(), bundle.getPropertyKeys());
                updateDependents(node, bundle.getBundleClass(), bundle.getRelations());
                return new Mutation<Vertex>(node, MutationState.UPDATED, nodeBundle);
            } else {
                logger.debug("Not updating equivalent bundle {}", bundle.getId());
                return new Mutation<Vertex>(node, MutationState.UNCHANGED);
            }
        } catch (SerializationError serializationError) {
            throw new RuntimeException("Unexpected serialization error " +
                    "checking bundle for equivalency", serializationError);
        }
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are not dependent are ignored.
     *
     * @param master
     * @param cls
     * @param relations
     * @return errors
     * @throws IntegrityError
     */
    private void createDependents(Vertex master,
            Class<?> cls, ListMultimap<String, Bundle> relations)
            throws IntegrityError {
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(cls);

        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle bundle : relations.get(relation)) {
                    Vertex child = createInner(bundle);
                    createChildRelationship(master, child, relation,
                            dependents.get(relation));
                }
            } else {
                logger.error("Nested data being ignored on creation because it is not a dependent relation: {}: {}", relation, relations.get(relation));
            }
        }
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are not dependent are ignored.
     *
     * @param master
     * @param cls
     * @param relations
     * @return errors
     * @throws ItemNotFound
     */
    private void updateDependents(Vertex master, Class<?> cls, ListMultimap<String,
            Bundle> relations) throws ItemNotFound {

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
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                Direction direction = dependents.get(relation);

                // FIXME: Assuming all dependents have the same direction of
                // relationship. This is *should* be safe, but could easily
                // break if the model ontology is altered without this
                // assumption in mind.
                HashSet<Vertex> currentRels = getCurrentRelationships(master,
                        direction, relation);

                for (Bundle bundle : relations.get(relation)) {
                    Vertex child = createOrUpdateInner(bundle).getNode();
                    // Create a relation if there isn't one already
                    if (!currentRels.contains(child)) {
                        createChildRelationship(master, child, relation,
                                direction);
                    }
                }
            } else {
                logger.warn("Nested data being ignored on update because " +
                        "it is not a dependent relation: {}: {}",
                        relation, relations.get(relation));
            }
        }
    }

    private Set<String> getUpdateSet(ListMultimap<String, Bundle> relations) {
        Set<String> updating = new HashSet<String>();
        for (String relation : relations.keySet()) {
            for (Bundle child : relations.get(relation)) {
                updating.add(child.getId());
            }
        }
        return updating;
    }

    private void deleteMissingFromUpdateSet(Vertex master,
            Map<String, Direction> dependents, Set<String> updating) {
        for (Entry<String, Direction> relEntry : dependents.entrySet()) {
            for (Vertex v : getCurrentRelationships(master,
                    relEntry.getValue(), relEntry.getKey())) {
                if (!updating.contains(manager.getId(v))) {
                    try {
                        delete(serializer.vertexFrameToBundle(graph.frame(v,
                                manager.getEntityClass(v).getEntityClass())));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Get the IDs of nodes that terminate a given relationship from a particular source node.
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
}
