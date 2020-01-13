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

package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * An entity that can access items and be granted permissions.
 *

 */
public interface Accessor extends Identifiable {

    @JavaHandler
    boolean isAdmin();

    @JavaHandler
    boolean isAnonymous();

    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    Iterable<Accessor> getParents();

    @JavaHandler
    Iterable<Accessor> getAllParents();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SUBJECT, direction=Direction.IN)
    Iterable<PermissionGrant> getPermissionGrants();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SUBJECT, direction=Direction.IN)
    void addPermissionGrant(PermissionGrant grant);

    abstract class Impl implements JavaHandlerContext<Vertex>, Accessor {

        public boolean isAdmin() {
            return it().getProperty(Ontology.IDENTIFIER_KEY).equals(Group.ADMIN_GROUP_IDENTIFIER);
        }

        public boolean isAnonymous() {
            return false;
        }

        public Iterable<Accessor> getAllParents() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
