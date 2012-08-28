package eu.ehri.project.persistance;

import java.lang.reflect.Method;
import java.util.HashMap;
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

import eu.ehri.project.core.Neo4jHelpers;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.Dependent;

public class BundlePersister<T extends VertexFrame> {

    private final FramedGraph<Neo4jGraph> graph;
    private final Neo4jHelpers helpers;

    public BundlePersister(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        this.helpers = new Neo4jHelpers(graph.getBaseGraph().getRawGraph());
    }
    
    private void saveDependents(MultiValueMap deps, Vertex master,
            Class<? extends VertexFrame> cls) throws ValidationError {
        for (Object key : deps.keySet()) {
            String relation = (String) key;
            for (Object obj : deps.getCollection(key)) {
                EntityBundle<? extends VertexFrame> bundle = (EntityBundle<?>) obj;
                Vertex child = insertOrUpdate(bundle);
                Direction direction = directionOfRelationship(cls, bundle.getBundleClass(), relation);
                // if (!master.getVertices(direction, (String)key).
                // TODO: Check if the relationship already exists...
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

    private Direction directionOfRelationship(
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

    public T persist(EntityBundle<T> bundle) throws ValidationError {
        // This should handle logic for setting
        // revisions, created, update dates, and all
        // that jazz...
        graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Vertex node = insertOrUpdate(bundle);
            saveDependents(bundle.getSaveWith(), node, bundle.getBundleClass());
            return graph.frame(node, bundle.getBundleClass());
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
}
