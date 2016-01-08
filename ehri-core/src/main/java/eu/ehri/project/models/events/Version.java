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

package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing a serialized version of
 * some other node.
 */
@EntityType(EntityClass.VERSION)
public interface Version extends Accessible {

    /**
     * Fetch the class of the entity that this version pertains to.
     *
     * @return an entity class enum value
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_CLASS)
    String getEntityType();

    /**
     * Fetch the ID of the entity that this version pertains to.
     *
     * @return an ID string
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_ID)
    String getEntityId();

    /**
     * Fetch a serialized snapshot of the item's data in JSON format.
     *
     * @return JSON data representing a sub-graph
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_DATA)
    String getEntityData();

    /**
     * Fetch the event that triggered this version.
     *
     * @return a system event instance
     */
    @Fetch(value = Ontology.VERSION_HAS_EVENT, ifLevel = 0)
    @Adjacency(label = Ontology.VERSION_HAS_EVENT, direction = Direction.OUT)
    SystemEvent getTriggeringEvent();

    /**
     * Loops up through the chain of versions until the latest and fetches
     * the item to which the events refer. If it has been
     *
     * @return the entity to which this version refers.
     */
    @JavaHandler
    Accessible getEntity();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Version {
        public Accessible getEntity() {
            Pipeline<Vertex,Vertex> out =  gremlin().as("n").in(Ontology.ENTITY_HAS_PRIOR_VERSION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.IN,
                                    Ontology.ENTITY_HAS_PRIOR_VERSION).iterator().hasNext();
                        }
                    });
            return (Accessible)(out.hasNext() ? frame(out.next()) : null);
        }
    }
}
