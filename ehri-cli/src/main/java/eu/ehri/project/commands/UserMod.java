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

package eu.ehri.project.commands;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.ActionManager.EventContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Modify an existing user.
 */
public class UserMod extends BaseCommand {

    final static String NAME = "usermod";

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("group")
                .hasArg()
                .type(String.class)
                .desc("A group to add the user to")
                .build());
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .required()
                .type(String.class)
                .hasArg().desc("Identifier of user taking action")
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
        return "Add an existing user to a group";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError,
            PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = cmdLine.getOptionValue("c",
                "Adding user to groups");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        // Fetch the admin accessor, who's going to do the work.
        Actioner admin = manager.getEntity(cmdLine.getOptionValue("user"), Actioner.class);

        String userId = cmdLine.getArgList().get(0);

        String[] groups = {};
        if (cmdLine.hasOption("group")) {
            groups = cmdLine.getOptionValues("group");
        }

        UserProfile user = manager.getEntity(userId,
                EntityClass.USER_PROFILE, UserProfile.class);

        EventContext actionCtx = new ActionManager(graph).newEventContext(
                user, admin, EventTypes.modification,
                getLogMessage(logMessage));

        for (String groupId : groups) {
            Group group = manager.getEntity(groupId, EntityClass.GROUP,
                    Group.class);
            group.addMember(user);
            actionCtx.addSubjects(group);
        }
        actionCtx.commit();

        return 0;
    }
}
