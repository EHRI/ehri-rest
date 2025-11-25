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
import eu.ehri.project.api.Api;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Add a user with optional group membership.
 */
public class UserAdd extends BaseCommand {

    final static String NAME = "useradd";

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("group")
                .hasArg()
                .type(String.class)
                .desc("A group to add the user to")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("name")
                .hasArg()
                .type(String.class)
                .desc("User's full name")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for update action.")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] <user-identifier>", NAME);
    }

    @Override
    public String getHelp() {
        return "Create a new user, and optionally add them to a group";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().isEmpty())
            throw new RuntimeException(getUsage());

        // Fetch the admin accessor, who's going to do the work.
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER,
                Accessor.class);

        String userId = cmdLine.getArgList().get(0);
        String userName = cmdLine.getOptionValue("n", userId);
        String[] groups = {};
        if (cmdLine.hasOption("group")) {
            groups = cmdLine.getOptionValues("group");
        }

        Bundle bundle = Bundle.of(EntityClass.USER_PROFILE,
                Maps.newHashMap())
                .withDataValue(Ontology.IDENTIFIER_KEY, userId)
                .withDataValue(Ontology.NAME_KEY, userName);
        String nodeId = EntityClass.USER_PROFILE.getIdGen()
                .generateId(SystemScope.getInstance().idPath(), bundle);
        bundle = bundle.withId(nodeId);

        Api api = api(graph, admin);
        UserProfile newUser = api
                .create(bundle, UserProfile.class, getLogMessage(logMessage));
        for (String groupId : groups) {
            Group group = manager.getEntity(groupId, EntityClass.GROUP, Group.class);
            api.acl().addAccessorToGroup(group, newUser);
        }

        return 0;
    }
}
