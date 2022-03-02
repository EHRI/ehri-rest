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

package eu.ehri.project.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Identifiable;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.events.SystemEventQueue;
import eu.ehri.project.models.idgen.IdGeneratorUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;

import static eu.ehri.project.models.EntityClass.CVOC_CONCEPT;
import static eu.ehri.project.models.EntityClass.DOCUMENTARY_UNIT;
import static eu.ehri.project.models.EntityClass.HISTORICAL_AGENT;
import static eu.ehri.project.models.EntityClass.REPOSITORY;
import static eu.ehri.project.persistence.ActionManager.GLOBAL_EVENT_ROOT;

/**
 * Sanity check various parts of the graph.
 */
public class Check extends BaseCommand {

    final static String NAME = "check";
    private static final String QUICK = "quick";

    @Override
    public String getHelp() {
        return "Perform various checks on the graph structure";
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS]";
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt(QUICK)
                .desc("Run fast, basic sanity checks only")
                .build());
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        checkInitialization(graph, manager);
        if (!cmdLine.hasOption(QUICK)) {
            checkPermissionScopes(graph, manager);
            checkOwnerPermGrantsHaveNoScope(manager);
        }

        return 0;
    }

    private void checkInitialization(FramedGraph<?> graph, GraphManager manager) {
        if (graph.getBaseGraph().getVertices().iterator().hasNext()) {
            try {
                SystemEventQueue queue =
                        manager.getEntity(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM, SystemEventQueue.class);
                if (!queue.getSystemEvents().iterator().hasNext()) {
                    System.err.println("Global event iterator is empty!");
                }
            } catch (ItemNotFound itemNotFound) {
                System.err.println("Unable to read event root in graph!");
            }

            try {
                manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Group.class);

                for (PermissionType pt : PermissionType.values()) {
                    manager.getEntity(pt.getName(), Permission.class);
                }
                for (ContentTypes ct : ContentTypes.values()) {
                    manager.getEntity(ct.getName(), ContentType.class);
                }
            } catch (ItemNotFound itemNotFound) {
                System.err.println("Unable to find item in graph with id: " + itemNotFound.getId());
            }
        } else {
            System.err.println("Graph contains no vertices (has it been initialized?)");
        }
    }

    /**
     * The following types of item should ALL have a permission scope.
     * <p>
     * Doc unit - either a repository or another doc unit
     * Concept - a vocabulary
     * Repository - a country
     * Hist agent - an auth set
     *
     * @param graph   The graph
     * @param manager The graph manager
     */
    private void checkPermissionScopes(FramedGraph<?> graph,
            GraphManager manager) {

        List<EntityClass> types = Lists.newArrayList(DOCUMENTARY_UNIT, REPOSITORY, CVOC_CONCEPT, HISTORICAL_AGENT);

        for (EntityClass entityClass : types) {
            try (CloseableIterable<? extends Entity> items = manager.getEntities(entityClass, entityClass.getJavaClass())) {
                for (Entity item : items) {
                    Accessible entity = item.as(Accessible.class);
                    PermissionScope scope = entity.getPermissionScope();
                    if (scope == null) {
                        System.err.println("Missing scope: " + entity.getId() + " (" + entity.asVertex().getId() + ")");
                    } else {
                        switch (manager.getEntityClass(item)) {
                            case DOCUMENTARY_UNIT:
                                checkIdGeneration(graph.frame(item.asVertex(), DocumentaryUnit.class), scope);
                                break;
                            case REPOSITORY:
                                checkIdGeneration(graph.frame(item.asVertex(), Repository.class), scope);
                                break;
                            case CVOC_CONCEPT:
                                checkIdGeneration(graph.frame(item.asVertex(), Concept.class), scope);
                            case HISTORICAL_AGENT:
                                checkIdGeneration(graph.frame(item.asVertex(), Concept.class), scope);
                            default:
                        }
                    }
                }
            }
        }
    }

    private void checkIdGeneration(Identifiable doc, PermissionScope scope) {
        if (scope != null) {
            String ident = doc.getIdentifier();
            List<String> path = Lists.newArrayList(Iterables.concat(scope.idPath(), Lists.newArrayList(ident)));
            String finalId = IdGeneratorUtils.joinPath(path);
            if (!finalId.equals(doc.getId())) {
                System.err.println(String.format("Generated ID does not match scopes: '%s' -> %s + %s",
                        doc.getId(), path, ident));
            }
        }
    }

    private void checkOwnerPermGrantsHaveNoScope(GraphManager manager) {
        try (CloseableIterable<PermissionGrant> items = manager
                .getEntities(EntityClass.PERMISSION_GRANT, PermissionGrant.class)) {
            for (PermissionGrant grant : items) {
                Entity scope = grant.getScope();
                Entity perm = grant.getPermission();
                if (scope != null && perm != null && perm.getId().equals(PermissionType.OWNER.getName())) {
                    System.err.println(
                            String.format("Owner permission grant with scope: %s", grant.asVertex().getId()));
                }
            }
        }

    }
}
