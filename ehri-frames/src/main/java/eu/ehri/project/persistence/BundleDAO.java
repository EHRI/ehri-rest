/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.persistence;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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

import java.util.Collection;
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
     * @param graph The graph
     * @param scopeIds The ID set for the current scope.
     */
    public BundleDAO(FramedGraph<?> graph, Collection<String> scopeIds) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).dependentOnly().build();
        validator = new BundleValidator(manager, scopeIds);
    }

    /**
     * Constructor with system scope.
     *
     * @param graph The graph
     */
    public BundleDAO(FramedGraph<?> graph) {
        this(graph, Lists.<String>newArrayList());
    }

    /**
     * Entry-point for updating a bundle.
     *
     * @param bundle The bundle to create or update
     * @param cls The frame class of the return type
     * @return A framed vertex mutation
     * @throws ValidationError
     * @throws ItemNotFound
     */
    public <T extends Frame> Mutation<T> update(Bundle bundle, Class<T> cls)
            throws ValidationError, ItemNotFound {
        Bundle bundleWithIds = validator.validateForUpdate(bundle);
        Mutation<Vertex> mutation = updateInner(bundleWithIds);
        return new Mutation<>(graph.frame(mutation.getNode(), cls),
                mutation.getState(), mutation.getPrior());
    }

    /**
     * Entry-point for creating a bundle.
     *
     * @param bundle The bundle to create or update
     * @param cls The frame class of the return type
     * @return A framed vertex
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
     * @param bundle The bundle to create or update
     * @param cls The frame class of the return type
     * @return A frame mutation
     * @throws ValidationError
     */
    public <T extends Frame> Mutation<T> createOrUpdate(Bundle bundle, Class<T> cls)
            throws ValidationError {
        Bundle bundleWithIds = validator.validateForUpdate(bundle);
        Mutation<Vertex> vertexMutation = createOrUpdateInner(bundleWithIds);
        return new Mutation<>(graph.frame(vertexMutation.getNode(), cls), vertexMutation.getState(),
                vertexMutation.getPrior());
    }

    /**
     * Delete a bundle and dependent items, returning the total number of vertices deleted.
     *
     * @param bundle The bundle to delete
     * @return The number of vertices deleted
     */
    public int delete(Bundle bundle) {
        try {
            return deleteCount(bundle, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helpers
    private int deleteCount(Bundle bundle, int count) throws Exception {
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
     * @param bundle The bundle to create or update
     * @return A vertex mutation
     * @throws RuntimeException when an item is said to exist, but could not be found
     */
    private Mutation<Vertex> createOrUpdateInner(Bundle bundle) {
        try {
            if (manager.exists(bundle.getId())) {
                return updateInner(bundle);
            } else {
                return new Mutation<>(createInner(bundle), MutationState.CREATED);
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
            createDependents(node, bundle.getBundleJavaClass(), bundle.getRelations());
            return node;
        } catch (IntegrityError e) {
            // Mmmn, if we get here, it means that there's been an ID generation error
            // which was not handled by an exception.. so throw a runtime error...
            throw new RuntimeException(
                    "Unexpected state: ID generation error not handled by IdGenerator class " + e.getMessage());
        }
    }

    /**
     * Update a bundle and save its dependent items.
     *
     * @param bundle The bundle to update
     * @return A vertex mutation
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
                updateDependents(node, bundle.getBundleJavaClass(), bundle.getRelations());
                return new Mutation<>(node, MutationState.UPDATED, nodeBundle);
            } else {
                logger.debug("Not updating equivalent bundle {}", bundle.getId());
                return new Mutation<>(node, MutationState.UNCHANGED);
            }
        } catch (SerializationError serializationError) {
            throw new RuntimeException("Unexpected serialization error " +
                    "checking bundle for equivalency", serializationError);
        }
    }

    /**
     * Saves the dependent relations within a given bundle. Relations that are not dependent are ignored.
     *
     * @param master The master vertex
     * @param cls The master vertex class
     * @param relations A map of relations
     * @throws IntegrityError
     */
    private void createDependents(Vertex master,
            Class<?> cls, Multimap<String, Bundle> relations)
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
     * @param master The master vertex
     * @param cls The master vertex class
     * @param relations A map of relations
     * @throws ItemNotFound
     */
    private void updateDependents(Vertex master, Class<?> cls, Multimap<String,
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
                Set<Vertex> currentRels = getCurrentRelationships(master,
                        relation, direction);

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

    private Set<String> getUpdateSet(Multimap<String, Bundle> relations) {
        Set<String> updating = new HashSet<>();
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
                    relEntry.getKey(), relEntry.getValue())) {
                if (!updating.contains(manager.getId(v))) {
                    try {
                        delete(serializer.vertexFrameToBundle(graph.frame(v,
                                manager.getEntityClass(v).getJavaClass())));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Get the nodes that terminate a given relationship from a particular source node.
     *
     *
     * @param src The source vertex
     * @param label The relationship label
     * @param direction The relationship direction
     * @return A set of related nodes
     */
    private Set<Vertex> getCurrentRelationships(Vertex src,
            String label, Direction direction) {
        HashSet<Vertex> out = Sets.newHashSet();
        for (Vertex end : src.getVertices(direction, label)) {
            out.add(end);
        }
        return out;
    }

    /**
     * Create a relationship between a parent and child vertex.
     *
     * @param master The master vertex
     * @param child The child vertex
     * @param label The relationship label
     * @param direction The direction of the relationship
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
