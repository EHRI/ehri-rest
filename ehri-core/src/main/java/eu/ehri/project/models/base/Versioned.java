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

package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * An entity that may have prior versions.
 */
public interface Versioned extends Accessible {
    /**
     * Get the most recent version for this item, if one exists.
     *
     * @return a version frame, or null if there is no prior version
     */
    @Adjacency(label = Ontology.ENTITY_HAS_PRIOR_VERSION, direction = Direction.OUT)
    Version getPriorVersion();

    /**
     * Loops through the chain of prior versions until the oldest existing version.
     *
     * @return an iterable of prior item versions
     */
    @JavaHandler
    Iterable<Version> getAllPriorVersions();

    abstract class Impl implements JavaHandlerContext<Vertex>, Versioned {

        public Iterable<Version> getAllPriorVersions() {
            return frameVertices(gremlin().as("n").out(Ontology.ENTITY_HAS_PRIOR_VERSION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
