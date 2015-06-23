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

package eu.ehri.project.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.idgen.IdGeneratorUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;

import static eu.ehri.project.models.EntityClass.CVOC_CONCEPT;
import static eu.ehri.project.models.EntityClass.DOCUMENTARY_UNIT;
import static eu.ehri.project.models.EntityClass.HISTORICAL_AGENT;
import static eu.ehri.project.models.EntityClass.REPOSITORY;

/**
 * Sanity check various parts of the graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Check extends BaseCommand {

    final static String NAME = "check";

    public Check() {
    }
    
	@Override
	public String getHelp() {
        return "perform various checks on the graph structure" +
        		"\n" + getUsage();
	}
	
    @Override
    public String getUsage() {
		return "Usage: check [OPTIONS]";
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("q", "quick", false, "Quick checks only"));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        checkPermissionScopes(graph, manager);
        checkOwnerPermGrantsHaveNoScope(manager);

        return 0;
    }

    /**
     * The following types of item should ALL have a permission scope.
     *
     *  Doc unit - either a repository or another doc unit
     *  Concept - a vocabulary
     *  Repository - a country
     *  Hist agent - an auth set
     *
     * @param graph The graph
     * @param manager The graph manager
     * @throws Exception
     */
    public void checkPermissionScopes(FramedGraph<?> graph,
            GraphManager manager) throws Exception {

        List<EntityClass> types = Lists.newArrayList(DOCUMENTARY_UNIT, REPOSITORY, CVOC_CONCEPT, HISTORICAL_AGENT);

        for (EntityClass entityClass : types) {
            try (CloseableIterable<? extends Frame> items = manager.getFrames(entityClass, entityClass.getEntityClass())) {
                for (Frame item : items) {
                    AccessibleEntity entity = graph.frame(item.asVertex(), AccessibleEntity.class);
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
                            default:
                        }
                    }
                }
            }
        }
    }

    private void checkIdGeneration(IdentifiableEntity doc, PermissionScope scope) {
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

    private void checkOwnerPermGrantsHaveNoScope(GraphManager manager) throws Exception {
        try (CloseableIterable<PermissionGrant> items = manager
                .getFrames(EntityClass.PERMISSION_GRANT, PermissionGrant.class)) {
            for (PermissionGrant grant : items) {
                Frame scope = grant.getScope();
                Frame perm = grant.getPermission();
                if (scope != null && perm != null && perm.getId().equals(PermissionType.OWNER.getName())) {
                    System.err.println(
                            String.format("Owner permission grant with scope: %s", grant.asVertex().getId()));
                }
            }
        }

    }
}
