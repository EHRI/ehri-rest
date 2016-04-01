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

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Class representing the system event queue node, of which
 * there Will Be Only One.
 */
@EntityType(EntityClass.SYSTEM)
public interface SystemEventQueue extends Entity {

    String STREAM_START = Ontology.ACTIONER_HAS_LIFECYCLE_ACTION + "Stream";

    /**
     * Fetch the latest global event.
     *
     * @return a system event frame
     */
    @Adjacency(label = STREAM_START)
    SystemEvent getLatestEvent();

    /**
     * Get a stream of system events, latest first.
     *
     * @return an iterable of event frames
     */
    @JavaHandler
    Iterable<SystemEvent> getSystemEvents();

    abstract class Impl implements JavaHandlerContext<Vertex>, SystemEventQueue {
        public Iterable<SystemEvent> getSystemEvents() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n")
                    .out(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);
            return frameVertices(gremlin()
                    .out(STREAM_START).cast(Vertex.class)
                    .copySplit(gremlin(), otherPipe)
                    .exhaustMerge().cast(Vertex.class));
        }
    }
}
