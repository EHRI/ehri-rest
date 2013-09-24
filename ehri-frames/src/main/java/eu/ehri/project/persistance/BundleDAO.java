package eu.ehri.project.persistance;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Optional;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.idgen.IdGenerator;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.BundleError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for persisting and deleting a Bundle - a data structure representing a graph node and its relations
 * to be updated in a single batch.
 *
 * NB: This is complicated considerably because we need to catch, accumulate, and rethrow exceptions in the context of
 * the subtree to which they belong.
 *
 * TODO: Extensive clean-up of error-handing.
 *
 */
public final class BundleDAO {

    private static final Logger logger = LoggerFactory.getLogger(BundleDAO.class);

    private final FramedGraph<?> graph;
    private final PermissionScope scope;
    private final GraphManager manager;
    private final Serializer serializer;

    /**
     * Constructor with a given scope.
     *
     * @param graph
     * @param scope
     */
    public BundleDAO(FramedGraph<?> graph, PermissionScope scope) {
        this.graph = graph;
        this.scope = Optional.fromNullable(scope)
                .or(SystemScope.getInstance());
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    /**
     * Constructor with system scope.
     *
     * @param graph
     */
    public BundleDAO(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
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
        Mutation<Vertex> mutation = updateInner(bundle);
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
        return graph.frame(createInner(bundle), cls);
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

        Mutation<Vertex> mutation = createOrUpdateInner(bundle);
        return new Mutation<T>(graph.frame(mutation.getNode(), cls),
                mutation.getState(), mutation.getPrior());
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
    private Integer deleteCount(Bundle bundle, Integer count) throws Exception {
        Integer c = count;
        ListMultimap<String, Bundle> fetch = bundle.getRelations();
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(bundle.getBundleClass());
        for (String key : fetch.keySet()) {
            for (Bundle sub : fetch.get(key)) {
                // FIXME: Make it so we don't typically do this check for
                // Dependent relations
                if (dependents.containsKey(key)) {
                    c = deleteCount(sub, c);
                }
            }
        }
        manager.deleteVertex(bundle.getId());
        c += 1;
        return c;
    }

    /**
     * Insert or update an item depending on a) whether it has an ID, and b) whether it has an ID and already exists. If
     * import mode is not enabled an error will be thrown.
     *
     * @param bundle
     * @return
     * @throws ValidationError
     */
    private Mutation<Vertex> createOrUpdateInner(Bundle bundle) throws ValidationError {
        if (bundle.getId() == null) {
            return new Mutation(createInner(bundle), MutationState.CREATED);
        } else {
            try {
                if (manager.exists(bundle.getId())) {
                    return updateInner(bundle);
                } else {
                    return new Mutation(createInner(bundle), MutationState.CREATED);
                }
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
     */
    private Vertex createInner(Bundle bundle) throws ValidationError {
        IdGenerator idGen = bundle.getType().getIdgen();
        try {
            ListMultimap<String, String> errors = BundleValidatorFactory
                    .getInstance(manager, bundle).validate();
            String id = bundle.getId() != null ? bundle.getId() : idGen
                    .generateId(bundle.getType(), scope, bundle);
            Vertex node = manager.createVertex(id, bundle.getType(),
                    bundle.getData(), bundle.getPropertyKeys());
            ListMultimap<String, BundleError> nestedErrors = createDependents(node, bundle.getBundleClass(),
                    bundle.getRelations());
            if (!errors.isEmpty() || hasNestedErrors(nestedErrors)) {
                  throw new ValidationError(bundle, errors, nestedErrors);
            }
            return node;
        } catch (IntegrityError e) {
            // Convert integrity errors to validation errors
            idGen.handleIdCollision(bundle.getType(), scope, bundle);
            // Mmmn, if we get here, it means that there's been an ID generation error
            // which was not handled by an exception.. so throw a runtime error...
            throw new RuntimeException(
                    "Unexpected state: ID generation error not handled by IdGenerator class: " + idGen);
        }
    }

    /**
     * Update a bundle and save its dependent items.
     *
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws ItemNotFound
     */
    private Mutation<Vertex> updateInner(Bundle bundle) throws ValidationError,
            ItemNotFound {
        Vertex node = manager.getVertex(bundle.getId());
        try {
            Bundle nodeBundle = serializer.vertexFrameToBundle(node);
            if (!nodeBundle.equals(bundle)) {
                logger.trace("Bundles differ\n\n{}\n\n{}", bundle.toJson(), nodeBundle.toJson());
                ListMultimap<String, String> errors = BundleValidatorFactory
                        .getInstance(manager, bundle).validateForUpdate();
                node = manager.updateVertex(bundle.getId(), bundle.getType(),
                        bundle.getData(), bundle.getPropertyKeys());
                ListMultimap<String, BundleError> nestedErrors = updateDependents(node, bundle.getBundleClass(),
                        bundle.getRelations());
                if (!errors.isEmpty() || hasNestedErrors(nestedErrors)) {
                    throw new ValidationError(bundle, errors, nestedErrors);
                }
                return new Mutation(node, MutationState.UPDATED, nodeBundle);
            } else {
                logger.debug("Not updating equivalent bundle {}", bundle.getId());
                return new Mutation(node, MutationState.UNCHANGED);
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
     * @return
     * @throws IntegrityError
     *
     * @return errors
     */
    private ListMultimap<String, BundleError> createDependents(Vertex master,
            Class<?> cls, ListMultimap<String, Bundle> relations)
            throws IntegrityError {
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(cls);

        // Accumulate child errors before re-throwing...
        ListMultimap<String, BundleError> errors = LinkedListMultimap.create();
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle bundle : relations.get(relation)) {
                    try {
                        Vertex child = createInner(bundle);
                        createChildRelationship(master, child, relation,
                                dependents.get(relation));
                        // no errors, so put a placeholder there instead.
                        errors.put(relation, null);
                    } catch (ValidationError e) {
                        errors.put(relation, e);
                    }
                }
            } else {
                logger.error("Nested data being ignored on creation because it is not a dependent relation: {}: {}", relation, relations.get(relation));
            }
        }
        return errors;
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are not dependent are ignored.
     *
     * @param master
     * @param cls
     * @param relations
     * @return
     * @throws ItemNotFound
     *
     * @return errors
     */
    private ListMultimap<String, BundleError> updateDependents(Vertex master,
            Class<?> cls, ListMultimap<String, Bundle> relations)
            throws ItemNotFound {

        // Get a list of dependent relationships for this class, and their
        // directions.
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(cls);
        // Build a list of the IDs of existing dependents we're going to be
        // updating.
        Set<String> updating = getUpdateSet(relations);
        // Any that we're not going to update can have their subtrees deleted.
        deleteMissingFromUpdateSet(master, dependents, updating);

        // Accumulate child errors before re-throwing...
        ListMultimap<String, BundleError> errors = LinkedListMultimap.create();

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
                    try {
                        Vertex child = createOrUpdateInner(bundle).getNode();
                        // Create a relation if there isn't one already
                        if (!currentRels.contains(child)) {
                            createChildRelationship(master, child, relation,
                                    direction);
                        }
                        errors.put(relation, null);
                    } catch (ValidationError e) {
                        errors.put(relation, e);
                    }
                }
            } else {
                logger.warn("Nested data being ignored on update because " +
                        "it is not a dependent relation: {}: {}",
                        relation, relations.get(relation));
            }
        }

        return errors;
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

    /**
     * Search a tree of errors and determine if there's anything in it except for null values, which have to be there to
     * maintain item ordering.
     *
     * @param errors
     * @return
     */
    private boolean hasNestedErrors(ListMultimap<String, BundleError> errors) {
        for (BundleError e : errors.values()) {
            if (e != null) {
                return true;
            }
        }
        return false;
    }
}
