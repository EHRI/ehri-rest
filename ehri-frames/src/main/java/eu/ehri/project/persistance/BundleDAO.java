package eu.ehri.project.persistance;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Transaction;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.BundleError;
import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class responsible for persisting and deleting a Bundle - a data structure
 * representing a graph node and its relations to be updated in a single batch.
 * 
 * NB: This is complicated considerably because we need to catch, accumulate,
 * and rethrow exceptions in the context of the subtree to which they belong.
 * 
 * TODO: Extensive clean-up of error-handing.
 * 
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
     * @throws ItemNotFound
     */
    public <T extends VertexFrame> T update(Bundle bundle, Class<T> cls)
            throws ValidationError, ItemNotFound {
        return graph.frame(updateInner(bundle), cls);
    }

    /**
     * Entry-point for creating a bundle.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     */
    public <T extends VertexFrame> T create(Bundle bundle, Class<T> cls)
            throws ValidationError {        
        return graph.frame(createInner(bundle), cls);
    }

    /**
     * Entry point for creating or updating a bundle, depending on whether it
     * has a supplied id.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     */
    public <T extends VertexFrame> T createOrUpdate(Bundle bundle, Class<T> cls)
            throws ValidationError {
        return graph.frame(createOrUpdateInner(bundle), cls);
    }

    /**
     * Delete a bundle and dependent items, returning the total number of
     * vertices deleted.
     * 
     * @param bundle
     * @return
     */
    public Integer delete(Bundle bundle) {
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
     * Insert or update an item depending on a) whether it has an ID, and b)
     * whether it has an ID and already exists. If import mode is not enabled an
     * error will be thrown.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     */
    private Vertex createOrUpdateInner(Bundle bundle) throws ValidationError {
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
     */
    private Vertex createInner(Bundle bundle) throws ValidationError {
        ListMultimap<String, String> errors = BundleValidatorFactory
                .getInstance(bundle).validate();
        Vertex node = null;
        ListMultimap<String, BundleError> nestedErrors = LinkedListMultimap
                .create();
        try {
            String id = bundle.getId() != null ? bundle.getId() : bundle
                    .getType().getIdgen()
                    .generateId(bundle.getType(), scope, bundle.getData());

            node = manager.createVertex(id, bundle.getType(),
                    bundle.getData(), bundle.getPropertyKeys(),
                    bundle.getUniquePropertyKeys());
            nestedErrors = createDependents(node, bundle.getBundleClass(),
                    bundle.getRelations());
        } catch (IntegrityError e) {
            // Convert integrity errors to validation errors
            for (Entry<String, String> entry : e.getFields().entrySet()) {
                errors.put(entry.getKey(), MessageFormat.format(
                        Messages.getString("BundleDAO.uniquenessError"), //$NON-NLS-1$
                        entry.getValue()));
            }
        }

        if (!errors.isEmpty() || hasNestedErrors(nestedErrors)) {
            System.out.println(nestedErrors);
            throw new ValidationError(bundle, errors, nestedErrors);
        }
        return node;
    }

    /**
     * Update a bundle and save its dependent items.
     * 
     * @param bundle
     * @return
     * @throws ValidationError
     * @throws ItemNotFound
     */
    private Vertex updateInner(Bundle bundle) throws ValidationError,
            ItemNotFound {
        ListMultimap<String, String> errors = BundleValidatorFactory
                .getInstance(bundle).validateForUpdate();
        Vertex node = null;
        ListMultimap<String, BundleError> nestedErrors = LinkedListMultimap
                .create();
        try {
            node = manager.updateVertex(bundle.getId(), bundle.getType(),
                    bundle.getData(), bundle.getPropertyKeys(),
                    bundle.getUniquePropertyKeys());
            nestedErrors = updateDependents(node, bundle.getBundleClass(),
                    bundle.getRelations());
        } catch (IntegrityError e) {
            for (Entry<String, String> entry : e.getFields().entrySet()) {
                errors.put(entry.getKey(), MessageFormat.format(
                        Messages.getString("BundleDAO.uniquenessError"), //$NON-NLS-1$
                        entry.getValue()));
            }
        }
        if (!errors.isEmpty() || hasNestedErrors(nestedErrors)) {
            throw new ValidationError(bundle, errors, nestedErrors);
        }
        return node;
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are
     * not dependent are ignored.
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
            }
        }
        return errors;
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are
     * not dependent are ignored.
     * 
     * @param master
     * @param cls
     * @param relations
     * @return
     * @throws IntegrityError
     * @throws ItemNotFound
     * 
     * @return errors
     */
    private ListMultimap<String, BundleError> updateDependents(Vertex master,
            Class<?> cls, ListMultimap<String, Bundle> relations)
            throws IntegrityError, ItemNotFound {

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
                        Vertex child = createOrUpdateInner(bundle);
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
        Serializer serializer = new Serializer(graph);
        for (Entry<String, Direction> relEntry : dependents.entrySet()) {
            for (Vertex v : getCurrentRelationships(master,
                    relEntry.getValue(), relEntry.getKey())) {
                if (!updating.contains(manager.getId(v))) {
                    try {
                        delete(serializer.vertexFrameToBundle(graph.frame(v,
                                manager.getType(v).getEntityClass())));
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

    /**
     * Search a tree of errors and determine if there's anything in it except
     * for null values, which have to be there to maintain item ordering.
     * 
     * @param errors
     * @return
     */
    private boolean hasNestedErrors(ListMultimap<String, BundleError> errors) {
        for (BundleError e : errors.values()) {
            if (e != null)
                return true;
        }
        return false;
    }
}
