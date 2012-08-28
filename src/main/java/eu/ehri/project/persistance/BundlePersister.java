package eu.ehri.project.persistance;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.Dependent;

public class BundlePersister<T extends VertexFrame> {

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphHelpers helpers;

    public BundlePersister(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        this.helpers = new GraphHelpers(graph.getBaseGraph().getRawGraph());
    }
    
    private void saveDependents(Vertex master, Class<? extends VertexFrame> cls,
            MultiValueMap deps) throws ValidationError {

        Set<Long> existing = new HashSet<Long>();
        Set<Long> refreshed = new HashSet<Long>();
        
        for (Object key : deps.keySet()) {
            String relation = (String) key;

            for (Object obj : deps.getCollection(key)) {
                EntityBundle<? extends VertexFrame> bundle = (EntityBundle<?>) obj;
                Vertex child = saveInDepth(bundle);
                refreshed.add((Long)child.getId());
                Direction direction = getDirectionOfRelationship(cls, bundle.getBundleClass(), relation);
                
                // FIXME: Traversing all the current relations here (for 
                // every individual dependent) is very inefficient!
                List<Long> current = getCurrentRelationships(master, direction, relation);
                existing.addAll(current);
                
                // Create a relation if there isn't one already
                if (!current.contains((Long)child.getId())) {
                    Index<Edge> index;
                    try {
                        index = helpers.getIndex(relation, Edge.class);
                    } catch (IndexNotFoundException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }                
                    if (direction == Direction.OUT) {
                        helpers.createIndexedEdge((Long) master.getId(),
                                (Long) child.getId(), relation,
                                new HashMap<String, Object>(), index);
                    } else {
                        helpers.createIndexedEdge((Long) child.getId(),
                                (Long) master.getId(), relation,
                                new HashMap<String, Object>(), index);
                    }
                }
            }
        }
        // TODO: Clean up dependent items that have not been saved in the
        // current operation, and are therefore assumed deleted.
        existing.removeAll(refreshed);
        for (Long id : existing) {
            graph.removeVertex(graph.getVertex(id));
        }
    }
    
    private List<Long> getCurrentRelationships(final Vertex src, Direction direction, String label) {
        List<Long> out = new LinkedList<Long>();
        for (Vertex end : src.getVertices(direction, label)) {
            out.add((Long)end.getId());
        }        
        return out;
    }

    private Direction getDirectionOfRelationship(
            Class<? extends VertexFrame> classA,
            Class<? extends VertexFrame> classB, String rel) {
        for (Method method : classA.getMethods()) {
            Dependent dep = method.getAnnotation(Dependent.class);
            if (dep != null) {
                Adjacency adj = method.getAnnotation(Adjacency.class);
                if (adj != null && adj.label().equals(rel)) {
                    return adj.direction();
                }
            }
        }
        // If we get here then something has gone badly wrong, because the
        // correct direction could not be found. Maybe it's better to just
        // ignore saving the dependency in the long run?
        throw new RuntimeException(
                String.format(
                        "Unable to find the direction of relationship between dependent classes with relationship '%s': '%', '%'",
                        rel, classA.getName(), classB.getName()));
    }

    public Vertex update(EntityBundle<?> bundle) throws ValidationError {
        bundle.validate();
        Index<Vertex> index = helpers.getOrCreateIndex(bundle.getEntityType(),
                Vertex.class);
        return helpers.updateIndexedVertex(bundle.getId(), bundle.getData(),
                index);
    }

    public Vertex insert(EntityBundle<?> bundle) throws ValidationError {
        bundle.validate();
        Index<Vertex> index = helpers.getOrCreateIndex(bundle.getEntityType(),
                Vertex.class);
        return helpers.createIndexedVertex(bundle.getData(), index);
    }

    public Vertex insertOrUpdate(EntityBundle<?> bundle) throws ValidationError {
        if (bundle.getId() != null) {
            return update(bundle);
        } else {
            return insert(bundle);
        }
    }
    
    private Vertex saveInDepth(EntityBundle<? extends VertexFrame> bundle) throws ValidationError {
        Vertex node = insertOrUpdate(bundle);
        saveDependents(node, bundle.getBundleClass(), bundle.getSaveWith());
        return node;
    }

    public T persist(EntityBundle<T> bundle) throws ValidationError {
        // This should handle logic for setting
        // revisions, created, update dates, and all
        // that jazz...
        graph.getBaseGraph().getRawGraph().beginTx();
        try {
            return graph.frame(saveInDepth(bundle), bundle.getBundleClass());
        } catch (ValidationError err) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new ValidationError(err.getMessage());
        } catch (Exception err) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(err);
        } finally {
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        }
    }
    
    public Integer deleteCount(EntityBundle<?> bundle, Integer count) throws ValidationError {
        // Recursively blast everything away! Use with caution.
        Integer c = count;        
        MultiValueMap deps = bundle.getSaveWith();
        for (Object key : deps.keySet()) {
            for (Object obj : deps.getCollection(key)) {
                EntityBundle<? extends VertexFrame> sub = (EntityBundle<?>) obj;
                c += deleteCount(sub, c);
            }
        }
        if (bundle.id != null) {
            graph.removeVertex(graph.getVertex(bundle.id));
            c += 1;
        }                    
        return c;
    }

    public Integer delete(EntityBundle<?> bundle) throws ValidationError {
        // Recursively blast everything away! Use with caution.
        return deleteCount(bundle, 0);                
    }

}
