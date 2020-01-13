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

package eu.ehri.project.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.persistence.ActionManager;

import java.util.HashMap;

/**
 * Initialize the graph with a minimal set of vertices. This includes:
 * <p>
 * <ul>
 *     <li>an admin account</li>
 *     <li>permission nodes</li>
 *     <li>content type nodes</li>
 * </ul>
 */
public class GraphInitializer {
    private final GraphManager manager;

    private static final String INIT_MESSAGE = "Initialising graph";

    public GraphInitializer(FramedGraph<?> graph) {
        manager = GraphManagerFactory.getInstance(graph);
    }

    public void initialize() throws Exception {

        // Create the system node which is the head of the global event streams
        manager.createVertex(ActionManager.GLOBAL_EVENT_ROOT, EntityClass.SYSTEM,
                ImmutableMap.<String, Object>of(
                        // It might be useful to know when this graph was
                        // initialise. We can also put other metadata here.
                        Ontology.EVENT_TIMESTAMP, ActionManager.getTimestamp(),
                        Ontology.EVENT_LOG_MESSAGE, INIT_MESSAGE
                ));

        // Create admin account
        manager.createVertex(Group.ADMIN_GROUP_IDENTIFIER,
                EntityClass.GROUP, new HashMap<String, Object>() {
            {
                put(Ontology.IDENTIFIER_KEY, Group.ADMIN_GROUP_IDENTIFIER);
                put(Ontology.NAME_KEY, Group.ADMIN_GROUP_NAME);
            }
        });

        // Create permission nodes corresponding to the Enum values
        for (PermissionType pt : PermissionType.values()) {
            manager.createVertex(pt.getName(), EntityClass.PERMISSION,
                    Maps.<String, Object>newHashMap());
        }

        // Create content type nodes corresponding to the Enum values
        for (ContentTypes ct : ContentTypes.values()) {
            manager.createVertex(ct.getName(), EntityClass.CONTENT_TYPE,
                    Maps.<String, Object>newHashMap());
        }
    }
}
