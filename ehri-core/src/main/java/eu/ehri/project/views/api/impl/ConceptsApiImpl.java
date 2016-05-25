package eu.ehri.project.views.api.impl;

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.acl.PermissionUtils;
import eu.ehri.project.views.api.ConceptsApi;

import java.util.List;

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
    public Concept addRelatedConcepts(Concept concept, List<String> related) throws ItemNotFound, PermissionDenied {
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String id : related) {
            Concept other = manager.getEntity(id, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.addRelatedConcept(other);
        }
        log(concept, related, EventTypes.modification);
        return concept;

    }

    @Override
    public Concept removeRelatedConcepts(Concept concept, List<String> related) throws ItemNotFound, PermissionDenied {
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String id : related) {
            Concept other = manager.getEntity(id, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.removeRelatedConcept(other);
        }
        log(concept, related, EventTypes.modification);
        return concept;

    }

    @Override
    public Concept addNarrowerConcepts(Concept concept, List<String> narrower) throws ItemNotFound, PermissionDenied {
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String id : narrower) {
            Concept other = manager.getEntity(id, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.addNarrowerConcept(other);
        }
        log(concept, narrower, EventTypes.modification);
        return concept;
    }

    @Override
    public Concept removeNarrowerConcepts(Concept concept, List<String> narrower) throws ItemNotFound, PermissionDenied {
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        for (String id : narrower) {
            Concept other = manager.getEntity(id, Concept.class);
            helper.checkEntityPermission(other, accessor, PermissionType.UPDATE);
            concept.removeNarrowerConcept(other);
        }
        log(concept, narrower, EventTypes.modification);
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
            return Optional.absent();
        }
    }
}
