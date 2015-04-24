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

package eu.ehri.project.views;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.JavaHandlerUtils;

import java.util.List;

/**
 * View class for interacting with virtual units.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class VirtualUnitViews {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final AclManager aclManager;
    private final ViewHelper viewHelper;

    public VirtualUnitViews(FramedGraph<?> graph) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.aclManager = new AclManager(graph);
        this.viewHelper = new ViewHelper(graph);

    }

    public void moveIncludedUnits(VirtualUnit from, VirtualUnit to, Iterable<DocumentaryUnit> included,
            Accessor accessor) throws PermissionDenied {
        // Wrap this in a list as a precaution because it
        // will be iterated twice!
        List<DocumentaryUnit> items = Lists.newArrayList(included);
        removeIncludedUnits(from, items, accessor);
        addIncludedUnits(to, items, accessor);
    }

    /**
     * Add documentary units to be included in a virtual unit as child items.
     *
     * @param parent   The parent VU
     * @param included A set of child DUs
     * @param accessor The current user
     * @throws PermissionDenied
     */
    public void addIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included, Accessor accessor)
            throws PermissionDenied {
        viewHelper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        for (DocumentaryUnit unit : included) {
            parent.addIncludedUnit(unit);
        }
    }

    /**
     * Remove documentary units from a virtual unit as child items.
     *
     * @param parent   The parent VU
     * @param included A set of child DUs
     * @param accessor The current user
     * @throws PermissionDenied
     */
    public void removeIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included, Accessor accessor)
            throws PermissionDenied {
        viewHelper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        for (DocumentaryUnit unit : included) {
            parent.removeIncludedUnit(unit);
        }
    }

    /**
     * Find virtual collections to which this item belongs.
     *
     * @param item     An item (typically a documentary unit)
     * @param accessor The current user
     * @return A set of top-level virtual units
     */
    public Iterable<VirtualUnit> getVirtualCollections(Frame item, Accessor accessor) {

        // This is a relatively complicated traversal. We want to go from the item,
        // then to any descriptions, from those descriptions to any virtual units
        // that reference them, and then up to the top-level item. It is complicated
        // by the fact that the first encountered unit might actually be the top-level
        // item, in which case our loop traversal will miss it, so we have to combine
        // the result

        GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<>();
        Pipeline<Vertex, Vertex> otherPipe = pipe.start(item.asVertex())
                .in(Ontology.DESCRIPTION_FOR_ENTITY)
                .in(Ontology.VC_DESCRIBED_BY)
                .as("n").out(Ontology.VC_IS_PART_OF)
                .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                    @Override
                    public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                        return (!vertexLoopBundle.getObject()
                                .getEdges(Direction.OUT, Ontology.VC_IS_PART_OF)
                                .iterator().hasNext())
                                && manager.getEntityClass(vertexLoopBundle.getObject())
                                .equals(EntityClass.VIRTUAL_UNIT);
                    }
                });

        GremlinPipeline<Vertex, Vertex> out = new GremlinPipeline<Vertex, Vertex>(item.asVertex())
                .copySplit(new GremlinPipeline<Vertex, Object>(item.asVertex())
                        .in(Ontology.DESCRIPTION_FOR_ENTITY)
                        .in(Ontology.VC_DESCRIBED_BY), otherPipe)
                .exhaustMerge().cast(Vertex.class).filter(new PipeFunction<Vertex, Boolean>() {
                    @Override
                    public Boolean compute(Vertex vertex) {
                        return !vertex.getEdges(Direction.OUT, Ontology.VC_IS_PART_OF)
                                .iterator().hasNext();
                    }
                }).filter(aclManager.getAclFilterFunction(accessor));

        return graph.frameVertices(out, VirtualUnit.class);
    }

    /**
     * Find virtual collections to which this user belongs.
     *
     * @param user     An user (typically a documentary unit)
     * @param accessor The current user
     * @return A set of top-level virtual units
     */
    public Iterable<VirtualUnit> getVirtualCollectionsForUser(Frame user, Accessor accessor) {
        GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<>();
        Pipeline<Vertex, Vertex> filtered = pipe.start(user.asVertex())
                .in(Ontology.VC_HAS_AUTHOR)
                .filter(aclManager.getAclFilterFunction(accessor));

        return graph.frameVertices(filtered, VirtualUnit.class);
    }
}
