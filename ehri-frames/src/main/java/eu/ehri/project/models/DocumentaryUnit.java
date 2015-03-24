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
package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A frame class for graph nodes representing documentary
 * unit items.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AbstractUnit {

    static final Logger logger = LoggerFactory.getLogger(DocumentaryUnit.class);

    /**
     * Get the repository that holds this documentary unit.
     *
     * @return the Repository that holds this DocumentaryUnit
     */
    @Fetch(Ontology.DOC_HELD_BY_REPOSITORY)
    @JavaHandler
    public Repository getRepository();

    /**
     * Set the repository that holds this documentary unit.
     *
     * @param repository a repository instance
     */
    @JavaHandler
    public void setRepository(final Repository repository);

    /**
     * Get parent documentary unit, if any
     *
     * @return a DocumentaryUnit that is this DocumentaryUnit's parent or null
     */
    @Fetch(Ontology.DOC_IS_CHILD_OF)
    @Adjacency(label = Ontology.DOC_IS_CHILD_OF)
    public DocumentaryUnit getParent();

    /**
     * Add a child document to this one.
     *
     * @param child a documentary unit instance
     */
    @JavaHandler
    public void addChild(final DocumentaryUnit child);

    /**
     * Fetches a list of all ancestors (parent -> parent -> parent)
     *
     * @return an Iterable of DocumentaryUnits that are ancestors
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getAncestors();

    /**
     * Count the number of child units at the immediate lower
     * level (not counting grand-children and lower ancestors.)
     *
     * @return the number of immediate child items
     */
    @Meta(CHILD_COUNT)
    @JavaHandler
    public long getChildCount();

    /**
     * Get child documentary units
     *
     * @return an Iterable of DocumentaryUnits that are children
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getChildren();

    /**
     * Fetch <b>all</b> ancestor items, including children of
     * children to all depths.
     *
     * @return child items at all lower levels
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getAllChildren();

    /**
     * Get the description items for this documentary unit.
     *
     * @return a iterable of document descriptions
     */
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, DocumentaryUnit {

        public long getChildCount() {
            return gremlin().inE(Ontology.DOC_IS_CHILD_OF).count();
        }

        public Iterable<DocumentaryUnit> getChildren() {
            return frameVertices(gremlin().in(Ontology.DOC_IS_CHILD_OF));
        }

        public void addChild(final DocumentaryUnit child) {
            JavaHandlerUtils
                    .addSingleRelationship(child.asVertex(), it(), Ontology.DOC_IS_CHILD_OF);
        }

        public Iterable<DocumentaryUnit> getAllChildren() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_IS_CHILD_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }


        public void setRepository(final Repository repository) {
            // NB: Convenience methods that proxies addCollection (which
            // in turn maintains the child item cache.)
            repository.addCollection(frame(it(), DocumentaryUnit.class));
        }

        public Repository getRepository() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.OUT,
                                    Ontology.DOC_IS_CHILD_OF).iterator().hasNext();
                        }
                    });

            GremlinPipeline<Vertex, Vertex> out = gremlin().cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .exhaustMerge().out(Ontology.DOC_HELD_BY_REPOSITORY);

            return (Repository) (out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<DocumentaryUnit> getAncestors() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
