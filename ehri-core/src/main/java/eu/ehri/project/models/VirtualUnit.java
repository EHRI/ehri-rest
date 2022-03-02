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
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.JavaHandlerUtils;

import static eu.ehri.project.models.utils.JavaHandlerUtils.addSingleRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.addUniqueRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.removeAllRelationships;

/**
 * Virtual documentary unit. Note: a *virtual* unit can
 * have its own descriptions which do not refer to *actual*
 * doc units, but are structurally the same. However, the label
 * and direction is different in these cases, with the "purely
 * virtual" descriptions having an outgoing "describes" relationship
 * to the VU, whereas descriptions that describe real doc units have
 * an incoming "isDescribedBy" relationship from a VU. The difference
 * denotes ownership (dependency) which likewise controls cascading
 * deletions.
 */
@EntityType(EntityClass.VIRTUAL_UNIT)
public interface VirtualUnit extends AbstractUnit {

    @Meta(CHILD_COUNT)
    @JavaHandler
    int countChildren();

    @Fetch(Ontology.VC_IS_PART_OF)
    @Adjacency(label = Ontology.VC_IS_PART_OF)
    VirtualUnit getParent();

    /**
     * Add a child.
     *
     * @param child The child collection
     * @return Whether or not the operation was allowed.
     */
    @JavaHandler
    boolean addChild(VirtualUnit child);

    /**
     * Remove a child virtual unit from this one.
     *
     * @param child a virtual unit frame
     * @return whether or not the item was removed
     */
    @JavaHandler
    boolean removeChild(VirtualUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @JavaHandler
    Iterable<VirtualUnit> getAncestors();

    /**
     * Get the child virtual units subordinate to this one.
     *
     * @return an iterable of virtual unit frames
     */
    @JavaHandler
    Iterable<VirtualUnit> getChildren();

    /**
     * Fetch <b>all</b> child virtual units and their children
     * recursively.
     *
     * @return an iterable of virtual unit frames
     */
    @JavaHandler
    Iterable<VirtualUnit> getAllChildren();

    /**
     * Fetch documentary unit items included in this virtual unit.
     *
     * @return an iterable of documentary unit frames
     */
    @Fetch(value = Ontology.VC_INCLUDES_UNIT, full = true)
    @Adjacency(label = Ontology.VC_INCLUDES_UNIT, direction = Direction.OUT)
    Iterable<DocumentaryUnit> getIncludedUnits();

    /**
     * Get the repositories which hold the documentary unit items
     * included in this virtual unit.
     *
     * @return an iterable of repository frames
     */
    @JavaHandler
    Iterable<Repository> getRepositories();

    /**
     * Add a documentary unit to be included in this virtual unit.
     *
     * @param unit a documentary unit frame
     * @return whether or not the item was newly added
     */
    @JavaHandler
    boolean addIncludedUnit(DocumentaryUnit unit);

    /**
     * Remove a documentary unit item from this virtual unit.
     *
     * @param unit a documentary unit frame
     */
    @JavaHandler
    void removeIncludedUnit(DocumentaryUnit unit);

    /**
     * Fetch the author of this virtual unit.
     *
     * @return a user or group frame
     */
    @Fetch(value = Ontology.VC_HAS_AUTHOR, numLevels = 0)
    @Adjacency(label = Ontology.VC_HAS_AUTHOR, direction = Direction.OUT)
    Accessor getAuthor();

    /**
     * Set the author of this virtual unit.
     *
     * @param accessor a user or group frame
     */
    @JavaHandler
    void setAuthor(Accessor accessor);

    /**
     * Fetch the descriptions of this virtual unit.
     *
     * @return an iterable of documentary unit description frames
     */
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    Iterable<DocumentaryUnitDescription> getVirtualDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, VirtualUnit {

        @Override
        public void setAuthor(Accessor accessor) {
            addSingleRelationship(it(), accessor.asVertex(), Ontology.VC_HAS_AUTHOR);
        }

        @Override
        public void removeIncludedUnit(DocumentaryUnit unit) {
            removeAllRelationships(it(), unit.asVertex(), Ontology.VC_INCLUDES_UNIT);
        }

        @Override
        public Iterable<VirtualUnit> getChildren() {
            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF));
        }

        @Override
        public boolean addChild(VirtualUnit child) {
            if (child.asVertex().equals(it())) {
                // Self-referential.
                return false;
            }
            for (Vertex parent : traverseAncestors()) {
                if (child.asVertex().equals(parent)) {
                    // Loop
                    return false;
                }
            }

            return addUniqueRelationship(child.asVertex(), it(), Ontology.VC_IS_PART_OF);
        }

        @Override
        public boolean removeChild(VirtualUnit child) {
            return removeAllRelationships(child.asVertex(), it(), Ontology.VC_IS_PART_OF);
        }

        private GremlinPipeline<Vertex, Vertex> traverseAncestors() {
            return gremlin().as("n")
                    .out(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc);
        }

        @Override
        public Iterable<VirtualUnit> getAllChildren() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }

        @Override
        public Iterable<VirtualUnit> getAncestors() {
            return frameVertices(traverseAncestors());
        }

        @Override
        public boolean addIncludedUnit(DocumentaryUnit unit) {
            return addUniqueRelationship(it(), unit.asVertex(), Ontology.VC_INCLUDES_UNIT);
        }

        @Override
        public int countChildren() {
            long incCount = gremlin().outE(Ontology.VC_INCLUDES_UNIT).count();
            long vcCount = gremlin().inE(Ontology.VC_IS_PART_OF).count();
            return Math.toIntExact(incCount + vcCount);
        }
    }
}
