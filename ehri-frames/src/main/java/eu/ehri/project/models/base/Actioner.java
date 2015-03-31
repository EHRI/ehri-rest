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

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * An entity whose actions are recorded.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public interface Actioner extends NamedEntity {
    /**
     * Fetch a list of Actions for this user in newest-first order.
     * 
     * @return
     */
    @JavaHandler
    public Iterable<SystemEvent> getActions();

    @JavaHandler
    public Iterable<SystemEvent> getLatestAction();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Actioner {
        public Iterable<SystemEvent> getLatestAction() {
            return frameVertices(gremlin()
                    .out(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .out(Ontology.ACTION_HAS_EVENT));
        }

        public Iterable<SystemEvent> getActions() {
            return frameVertices(gremlin().as("n").out(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc)
                    .out(Ontology.ACTION_HAS_EVENT));
        }
    }
}
