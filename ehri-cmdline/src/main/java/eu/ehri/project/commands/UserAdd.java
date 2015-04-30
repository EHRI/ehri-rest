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

import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Add a user with optional group membership.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class UserAdd extends BaseCommand implements Command {

    final static String NAME = "useradd";

    public UserAdd() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("group", true,
           "A group to add the new user to"));
        options.addOption(new Option("n", "name", true, "User's full name"));
        options.addOption(new Option("c", "comment", false,
                "Log message for create action action."));
    }

    @Override
    public String getHelp() {
        return "Usage: useradd <user-identifier> [OPTIONS]";
    }

    @Override
    public String getUsage() {
        return "Create a new user, and optionally add them to a group";
    }

    @Override
    public int execWithOptions(final FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        // Fetch the admin accessor, who's going to do the work.
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                Accessor.class);

        String userId = (String) cmdLine.getArgList().get(0);
        String userName = cmdLine.getOptionValue("n", userId);
        String[] groups = {};
        if (cmdLine.hasOption("group")) {
            groups = cmdLine.getOptionValues("group");
        }

        Bundle bundle = new Bundle(EntityClass.USER_PROFILE,
                Maps.<String, Object> newHashMap())
                .withDataValue(Ontology.IDENTIFIER_KEY, userId)
                .withDataValue(Ontology.NAME_KEY, userName);
        String nodeId = EntityClass.USER_PROFILE.getIdgen()
                .generateId(SystemScope.getInstance().idPath(), bundle);
        bundle = bundle.withId(nodeId);

        LoggingCrudViews<UserProfile> view = new LoggingCrudViews<>(
                graph, UserProfile.class);
        UserProfile newUser = view.create(bundle, admin, getLogMessage(logMessage));
        for (String groupId : groups) {
            Group group = manager.getFrame(groupId, EntityClass.GROUP, Group.class);
            group.addMember(newUser);
        }

        return 0;
    }
}
