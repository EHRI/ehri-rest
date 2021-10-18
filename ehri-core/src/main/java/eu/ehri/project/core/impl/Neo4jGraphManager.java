/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Vertex;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes,
 * with Neo4j Lucene query optimisations.
 */
public final class Neo4jGraphManager<T extends Neo4j2Graph> extends BlueprintsGraphManager<T> {

    private final static Logger logger = LoggerFactory.getLogger(Neo4jGraphManager.class);

    public Neo4jGraphManager(FramedGraph<T> graph) {
        super(graph);
    }

    public static final String BASE_LABEL = "_Entity";

    @Override
    public boolean exists(String id) {
        Preconditions.checkNotNull(id,
                "attempt determine existence of a vertex with a null id");
        try (CloseableIterable<Vertex> q = graph.getBaseGraph()
                .getVerticesByLabelKeyValue(BASE_LABEL, EntityType.ID_KEY, id)) {
            return q.iterator().hasNext();
        }
    }

    @Override
    public Vertex getVertex(String id) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        try (CloseableIterable<Vertex> q = graph.getBaseGraph()
                .getVerticesByLabelKeyValue(BASE_LABEL, EntityType.ID_KEY, id)) {
            return q.iterator().next();
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        }
    }

    @Override
    public CloseableIterable<Vertex> getVertices(String key, Object value,
                                                 EntityClass type) {
        return graph.getBaseGraph().getVerticesByLabelKeyValue(type.getName(),
                key, value);
    }

    @Override
    public Vertex createVertex(String id, EntityClass type,
                               Map<String, ?> data) throws IntegrityError {
        return setLabels(super.createVertex(id, type, data));
    }

    @Override
    public Vertex updateVertex(String id, EntityClass type,
                               Map<String, ?> data) throws ItemNotFound {
        return setLabels(super.updateVertex(id, type, data));
    }

    @Override
    public void initialize() {
    }

    @Override
    public CloseableIterable<Vertex> getVertices(EntityClass type) {
        return graph.getBaseGraph().getVerticesByLabel(type.getName());
    }

    /**
     * Set labels on a Neo4j-based vertex.
     *
     * @param vertex the vertex
     * @return a vertex with labels set
     */
    public Vertex setLabels(Vertex vertex) {
        Neo4j2Vertex node = (Neo4j2Vertex) vertex;
        for (String label : node.getLabels()) {
            node.removeLabel(label);
        }
        node.addLabel(BASE_LABEL);
        String type = getType(vertex);
        node.addLabel(type);
        return node;
    }

    /**
     * Drop the existing schema.
     *
     * @param tx the underlying graph transactions
     */
    public static void dropIndicesAndConstraints(Transaction tx) {
        Schema schema = tx.schema();
        for (ConstraintDefinition constraintDefinition : schema.getConstraints()) {
            constraintDefinition.drop();
        }
        for (IndexDefinition indexDefinition : schema.getIndexes()) {
            indexDefinition.drop();
        }
    }

    /**
     * Create the graph schema
     *
     * @param tx the underlying graph transaction
     */
    public static void createIndicesAndConstraints(Transaction tx) {
        Schema schema = tx.schema();
        schema.constraintFor(Label.label(BASE_LABEL))
                .assertPropertyIsUnique(EntityType.ID_KEY)
                .create();
        schema.indexFor(Label.label(BASE_LABEL))
                .on(EntityType.TYPE_KEY)
                .create();

        // Create an index on each mandatory or indexed property and
        // a unique constraint on unique properties.
        for (EntityClass cls : EntityClass.values()) {
            Set<String> propertyKeys = Sets.newHashSet();
            propertyKeys.addAll(ClassUtils.getIndexedPropertyKeys(cls.getJavaClass()));
            propertyKeys.addAll(ClassUtils.getMandatoryPropertyKeys(cls.getJavaClass()));
            for (String prop : propertyKeys) {
                logger.trace("Creating index on property: {} -> {}", cls.getName(), prop);
                schema.indexFor(Label.label(cls.getName()))
                        .on(prop)
                        .create();
            }

            Collection<String> uniquePropertyKeys = ClassUtils.getUniquePropertyKeys(cls.getJavaClass());
            for (String unique : uniquePropertyKeys) {
                logger.trace("Creating constraint on unique property: {} -> {}",
                        cls.getName(), unique);
                schema.constraintFor(Label.label(cls.getName()))
                        .assertPropertyIsUnique(unique)
                        .create();
            }
        }
    }
}
