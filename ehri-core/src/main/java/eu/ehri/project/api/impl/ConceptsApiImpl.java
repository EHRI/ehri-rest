/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.api.impl;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.PermissionUtils;
import eu.ehri.project.api.ConceptsApi;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

class ConceptsApiImpl implements ConceptsApi {

    private final FramedGraph<?> graph;
    private final Accessor accessor;
    private final boolean logging;
    private final GraphManager manager;
    private final ActionManager actionManager;
    private final PermissionUtils helper;

    ConceptsApiImpl(FramedGraph<?> graph, Accessor accessor, boolean logging) {
        this.graph = graph;
        this.accessor = accessor;
        this.logging = logging;
        manager = GraphManagerFactory.getInstance(graph);
        actionManager = new ActionManager(graph);
        helper = new PermissionUtils(graph);
    }

    @Override
    public Concept addRelatedConcepts(String id, List<String> related) throws ItemNotFound, PermissionDenied {
        Concept concept = manager.getEntity(id, EntityClass.CVOC_CONCEPT, Concept.class);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String otherId : related) {
            Concept other = manager.getEntity(otherId, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.addRelatedConcept(other);
        }
        log(concept, related, EventTypes.modification);
        return concept;

    }

    @Override
    public Concept removeRelatedConcepts(String id, List<String> related) throws ItemNotFound, PermissionDenied {
        Concept concept = manager.getEntity(id, EntityClass.CVOC_CONCEPT, Concept.class);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String otherId : related) {
            Concept other = manager.getEntity(otherId, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.removeRelatedConcept(other);
        }
        log(concept, related, EventTypes.modification);
        return concept;

    }

    @Override
    public Concept addNarrowerConcepts(String id, List<String> narrower) throws ItemNotFound, PermissionDenied {
        Concept concept = manager.getEntity(id, EntityClass.CVOC_CONCEPT, Concept.class);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String otherId : narrower) {
            Concept other = manager.getEntity(otherId, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.addNarrowerConcept(other);
        }
        log(concept, narrower, EventTypes.modification);
        return concept;
    }

    @Override
    public Concept removeNarrowerConcepts(String id, List<String> narrower) throws ItemNotFound, PermissionDenied {
        Concept concept = manager.getEntity(id, EntityClass.CVOC_CONCEPT, Concept.class);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String otherId : narrower) {
            Concept other = manager.getEntity(otherId, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.removeNarrowerConcept(other);
        }
        log(concept, narrower, EventTypes.modification);
        return concept;
    }

    @Override
    public Concept setBroaderConcepts(String id, List<String> broader) throws ItemNotFound, PermissionDenied, DeserializationError {
        Concept concept = manager.getEntity(id, EntityClass.CVOC_CONCEPT, Concept.class);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (Concept other : concept.getBroaderConcepts()) {
            concept.removeBroaderConcept(other);
        }
        for (String otherId : broader) {
            Concept other = manager.getEntity(otherId, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            if (!Objects.equals(other.getAuthoritativeSet(), concept.getAuthoritativeSet())) {
                throw new DeserializationError("broader concepts must belong to the same vocabulary");
            }
            concept.addBroaderConcept(other);
        }
        log(concept, broader, EventTypes.modification);
        return concept;
    }


    private Optional<SystemEvent> log(Concept concept, List<String> ids, EventTypes type) throws ItemNotFound {
        if (logging && !ids.isEmpty()) {
            ActionManager.EventContext ctx = actionManager
                    .newEventContext(concept, accessor.as(Actioner.class), type);
            for (String id : ids) {
                ctx.addSubjects(manager.getEntity(id, Accessible.class));
            }
            return Optional.of(ctx.commit());
        } else {
            return Optional.empty();
        }
    }
}
