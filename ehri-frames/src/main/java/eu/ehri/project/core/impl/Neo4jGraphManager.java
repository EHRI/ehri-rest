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

package eu.ehri.project.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Vertex;
import eu.ehri.project.core.impl.neo4j.Neo4j2VertexIterable;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes,
 * with Neo4j Lucene query optimisations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class Neo4jGraphManager<T extends Neo4j2Graph> extends BlueprintsGraphManager<T> {

    public Neo4jGraphManager(FramedGraph<T> graph) {
        super(graph);
    }

    @Override
    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        String queryStr = getLuceneQuery(EntityType.ID_KEY, id, type.getName());
        // NB: Not using rawQuery.getSingle here so we throw NoSuchElement
        // other than return null.
        try (IndexHits<Node> rawQuery = getRawIndex().query(queryStr)) {
            return new Neo4j2Vertex(rawQuery.iterator().next(),
                    graph.getBaseGraph());
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        }
    }

    // NB: It's safe to do an unsafe cast here because we know that
    // Neo4j2Vertex extends Vertex.
    @Override
    @SuppressWarnings("unchecked")
    public CloseableIterable<Vertex> getVertices(String key, Object value,
            EntityClass type) {
        String queryStr = getLuceneQuery(key, value, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return (CloseableIterable<Vertex>) new Neo4j2VertexIterable(rawQuery,
                graph.getBaseGraph());
    }

    private org.neo4j.graphdb.index.Index<Node> getRawIndex() {
        IndexManager index = graph.getBaseGraph().getRawGraph().index();
        return index.forNodes(INDEX_NAME);
    }

    @Override
    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws IntegrityError {
        Neo4j2Vertex node = (Neo4j2Vertex)super.createVertex(id, type, data, keys);
        node.addLabel(type.getName());
        return node;
    }

    @Override
    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws ItemNotFound {
        Neo4j2Vertex node = (Neo4j2Vertex)super.updateVertex(id, type, data, keys);
        List<String> labels = Lists.newArrayList(node.getLabels());
        if (!(labels.size() == 1 && labels.get(0).equals(type.getName()))) {
            for (String label: labels) {
                node.removeLabel(label);
            }
            node.addLabel(type.getName());
        }
        return node;
    }

    private String getLuceneQuery(String key, Object value, String type) {
        return String.format("%s:\"%s\" AND %s:\"%s\"",
                QueryParser.escape(key),
                QueryParser.escape(String.valueOf(value)),
                QueryParser.escape(EntityType.TYPE_KEY),
                QueryParser.escape(type));
    }
}
