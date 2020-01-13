/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.annotations.UniqueAdjacency;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A frame class for graph nodes representing documentary
 * unit items.
 */
@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AbstractUnit {

    Logger logger = LoggerFactory.getLogger(DocumentaryUnit.class);

    /**
     * Get the repository that holds this documentary unit.
     *
     * @return the repository that holds this DocumentaryUnit
     */
    @Fetch(Ontology.DOC_HELD_BY_REPOSITORY)
    @JavaHandler
    Repository getRepository();

    /**
     * Get the repository if this item is at the top of its hierarchy.
     * Otherwise, return null.
     *
     * @return the repository, or null.
     */
    @Adjacency(label = Ontology.DOC_HELD_BY_REPOSITORY, direction = Direction.OUT)
    Repository getRepositoryIfTopLevel();

    /**
     * Set the repository that holds this documentary unit.
     *
     * @param repository a repository instance
     */
    @UniqueAdjacency(label = Ontology.DOC_HELD_BY_REPOSITORY, single = true)
    void setRepository(Repository repository);

    /**
     * Get parent documentary unit, if any
     *
     * @return a DocumentaryUnit that is this DocumentaryUnit's parent or null
     */
    @Fetch(Ontology.DOC_IS_CHILD_OF)
    @Adjacency(label = Ontology.DOC_IS_CHILD_OF)
    DocumentaryUnit getParent();

    /**
     * Add a child document to this one.
     *
     * @param child a documentary unit instance
     */
    @UniqueAdjacency(label = Ontology.DOC_IS_CHILD_OF, direction = Direction.IN, single = true)
    void addChild(DocumentaryUnit child);

    /**
     * Fetches a list of all ancestors (parent -&gt; parent -&gt; parent)
     *
     * @return an Iterable of DocumentaryUnits that are ancestors
     */
    @JavaHandler
    Iterable<DocumentaryUnit> getAncestors();

    /**
     * Get an iterable of ancestors, prefixed by this item.
     *
     * @return an iterable of DocumentaryUnit items including
     * the current item
     */
    @JavaHandler
    Iterable<DocumentaryUnit> getAncestorsAndSelf();

    /**
     * Get the immediate virtual units which include this item.
     *
     * @return an iterable of VirtualUnit items
     */
    @Adjacency(label = Ontology.VC_INCLUDES_UNIT, direction = Direction.IN)
    Iterable<VirtualUnit> getVirtualParents();

    /**
     * Get virtual collections to which this documentary unit belongs.
     *
     * @return an iterable of virtual unit objects at the top level
     */
    @JavaHandler
    Iterable<VirtualUnit> getVirtualCollections();

    /**
     * Count the number of child units at the immediate lower
     * level (not counting grand-children and lower ancestors.)
     *
     * @return the number of immediate child items
     */
    @Meta(CHILD_COUNT)
    @UniqueAdjacency(label = Ontology.DOC_IS_CHILD_OF, direction = Direction.IN)
    int countChildren();

    /**
     * Get child documentary units
     *
     * @return an Iterable of DocumentaryUnits that are children
     */
    @Adjacency(label = Ontology.DOC_IS_CHILD_OF, direction = Direction.IN)
    Iterable<DocumentaryUnit> getChildren();

    /**
     * Fetch <b>all</b> ancestor items, including children of
     * children to all depths.
     *
     * @return child items at all lower levels
     */
    @JavaHandler
    Iterable<DocumentaryUnit> getAllChildren();

    /**
     * Get the description items for this documentary unit.
     *
     * @return a iterable of document descriptions
     */
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    Iterable<DocumentaryUnitDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, DocumentaryUnit {

        public Iterable<DocumentaryUnit> getAllChildren() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_IS_CHILD_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }

        public Repository getRepository() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops,
                            vertexLoopBundle -> !vertexLoopBundle.getObject().getVertices(Direction.OUT,
                                    Ontology.DOC_IS_CHILD_OF).iterator().hasNext());

            GremlinPipeline<Vertex, Vertex> out = gremlin().cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .exhaustMerge().out(Ontology.DOC_HELD_BY_REPOSITORY);

            return (Repository) (out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<DocumentaryUnit> getAncestors() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }

        public Iterable<DocumentaryUnit> getAncestorsAndSelf() {
            GremlinPipeline<Vertex, Vertex> ancestors = gremlin()
                    .as("n")
                    .out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc);

            GremlinPipeline<Vertex, Vertex> all = gremlin().cast(Vertex.class)
                    .copySplit(gremlin(), ancestors).exhaustMerge().cast(Vertex.class);

            return frameVertices(all);
        }

        public Iterable<VirtualUnit> getVirtualCollections() {
            GremlinPipeline<Vertex, ?> ancestors = gremlin()
                    .as("n")
                    .out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc);

            GremlinPipeline<Vertex, ?> ancestorsAndSelf = gremlin()
                    .cast(Vertex.class)
                    .copySplit(gremlin(), ancestors)
                    .exhaustMerge();


            Pipeline<Vertex, Vertex> all = ancestorsAndSelf
                    .in(Ontology.VC_INCLUDES_UNIT)
                    .as("n").out(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, vertexLoopBundle ->
                            (!vertexLoopBundle.getObject()
                                    .getEdges(Direction.OUT, Ontology.VC_IS_PART_OF)
                                    .iterator().hasNext())
                                    && EntityClass.VIRTUAL_UNIT
                                    .getName()
                                    .equals(vertexLoopBundle.getObject()
                                            .getProperty(EntityType.TYPE_KEY)))
                    .cast(Vertex.class);

            return frameVertices(all);
        }
    }
}
