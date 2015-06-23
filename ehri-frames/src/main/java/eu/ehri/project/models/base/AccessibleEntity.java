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

package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import static eu.ehri.project.models.utils.JavaHandlerUtils.addSingleRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.addUniqueRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.hasEdge;

/**
 * An entity that can be accessed by specific {@link Accessor}s.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public interface AccessibleEntity extends PermissionGrantTarget, VersionedEntity, AnnotatableEntity {

    @Fetch(value = Ontology.IS_ACCESSIBLE_TO, ifBelowLevel = 1)
    @Adjacency(label = Ontology.IS_ACCESSIBLE_TO)
    Iterable<Accessor> getAccessors();

    /**
     * only Accessor accessor can access this AccessibleEntity.
     * This is NOT the way to add an Accessor to a Group, use Group.addMember()
     * @param accessor 
     */
    @JavaHandler
    void addAccessor(Accessor accessor);

    @Adjacency(label = Ontology.IS_ACCESSIBLE_TO)
    void removeAccessor(Accessor accessor);

    @Adjacency(label = Ontology.HAS_PERMISSION_SCOPE)
    PermissionScope getPermissionScope();

    @JavaHandler
    void setPermissionScope(PermissionScope scope);

    @JavaHandler
    Iterable<PermissionScope> getPermissionScopes();

    /**
     * Fetch a list of Actions for this entity in order.
     * 
     * @return
     */
    @JavaHandler
    Iterable<SystemEvent> getHistory();

    @Fetch(value = Ontology.ENTITY_HAS_LIFECYCLE_EVENT, ifLevel = 0)
    @JavaHandler
    SystemEvent getLatestEvent();

    @JavaHandler
    boolean hasAccessRestriction();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, AccessibleEntity {

        public void addAccessor(Accessor accessor) {
            addUniqueRelationship(it(), accessor.asVertex(),
                    Ontology.IS_ACCESSIBLE_TO);
        }

        public void setPermissionScope(PermissionScope scope) {
            addSingleRelationship(it(), scope.asVertex(),
                    Ontology.HAS_PERMISSION_SCOPE);
        }

        public SystemEvent getLatestEvent() {
            GremlinPipeline<Vertex, Vertex> out = gremlin()
                    .out(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .out(Ontology.ENTITY_HAS_EVENT);
            return (SystemEvent)(out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<PermissionScope> getPermissionScopes() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }

        public Iterable<SystemEvent> getHistory() {
            return frameVertices(gremlin().as("n").out(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc)
                    .out(Ontology.ENTITY_HAS_EVENT));
        }

        public boolean hasAccessRestriction() {
            return hasEdge(it(), Direction.OUT, Ontology.IS_ACCESSIBLE_TO)
                    && !hasEdge(it(), Direction.OUT, Ontology.PROMOTED_BY);
        }
    }
}
