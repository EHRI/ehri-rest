package eu.ehri.project.core.impl;

import com.google.common.base.Preconditions;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertexIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes,
 * with Neo4j Lucene query optimisations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class Neo4jGraphManager<T extends Neo4jGraph> extends BlueprintsGraphManager<T> implements GraphManager {

    public Neo4jGraphManager(FramedGraph<T> graph) {
        super(graph);
    }

    @Override
    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        String queryStr = getLuceneQuery(EntityType.ID_KEY, id, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        // NB: Not using rawQuery.getSingle here so we throw NoSuchElement
        // other than return null.
        try {
            return new Neo4jVertex(rawQuery.iterator().next(),
                    graph.getBaseGraph());
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        } finally {
            rawQuery.close();
        }
    }

    // NB: It's safe to do an unsafe cast here because we know that
    // Neo4jVertex extends Vertex.
    @Override
    @SuppressWarnings("unchecked")
    public CloseableIterable<Vertex> getVertices(String key, Object value,
            EntityClass type) {
        String queryStr = getLuceneQuery(key, value, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return (CloseableIterable<Vertex>) new Neo4jVertexIterable(rawQuery,
                graph.getBaseGraph(), false);
    }

    private org.neo4j.graphdb.index.Index<Node> getRawIndex() {
        IndexManager index = graph.getBaseGraph().getRawGraph().index();
        return index.forNodes(INDEX_NAME);
    }

    private String getLuceneQuery(String key, Object value, String type) {
        return String.format("%s:\"%s\" AND %s:\"%s\"",
                QueryParser.escape(key),
                QueryParser.escape(String.valueOf(value)),
                QueryParser.escape(EntityType.TYPE_KEY),
                QueryParser.escape(type));
    }
}
