package eu.ehri.project.persistance;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.core.Neo4jHelpers;
import eu.ehri.project.exceptions.ValidationError;

public class BundlePersister<T extends VertexFrame> {

    private final FramedGraph<Neo4jGraph> graph;
    private final Neo4jHelpers helpers;

    public BundlePersister(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        this.helpers = new Neo4jHelpers(graph.getBaseGraph().getRawGraph());
    }
    
    public T persist(EntityUpdateBundle<T> bundle) throws ValidationError {
        bundle.validate();
        Index<Vertex> index = helpers.getOrCreateIndex(bundle.getEntityType(), Vertex.class);
        Vertex node = helpers.updateIndexedVertex(bundle.getId(), bundle.getData(), index);
        return graph.frame(node, bundle.getBundleClass());
    }

    public T persist(EntityBundle<T> bundle) throws ValidationError {
        // This should handle logic for setting
        // revisions, created, update dates, and all
        // that jazz...
        bundle.validate();
        Index<Vertex> index = helpers.getOrCreateIndex(bundle.getEntityType(), Vertex.class);
        Vertex node = helpers.createIndexedVertex(bundle.getData(), index);
        return graph.frame(node, bundle.getBundleClass());
    }
}
