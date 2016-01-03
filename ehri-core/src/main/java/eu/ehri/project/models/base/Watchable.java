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
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.Meta;

import static eu.ehri.project.definitions.Ontology.USER_WATCHING_ITEM;

/**
 * An entity that can be watched by users.
 */
public interface Watchable extends Accessible {
    String WATCHED_COUNT = "watchedBy";

    @Adjacency(label = USER_WATCHING_ITEM, direction = Direction.IN)
    Iterable<UserProfile> getWatchers();

    @Meta(WATCHED_COUNT)
    @JavaHandler
    long getWatchedCount();

    abstract class Impl implements JavaHandlerContext<Vertex>, Watchable {

        @Override
        public long getWatchedCount() {
            return gremlin().inE(USER_WATCHING_ITEM).count();
        }
    }
}
