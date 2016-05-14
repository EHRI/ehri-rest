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

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.views.impl.CrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Delete an entire class of entities via the command line.
 */
public class DeleteEntities extends BaseCommand {

    final static String NAME = "delete-all";

    @Override
    protected void setCustomOptions(Options options) {
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
                .desc("Log message for delete action.")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] <type>", NAME);
    }

    @Override
    public String getHelp() {
        return "Delete ALL entities of a given type.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        // the first argument is the entity type, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getJavaClass();

        String logMessage = "Deleting items of type " + type + " via the command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        if (!Accessible.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        // Find the user
        UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"),
                UserProfile.class);

        new ActionManager(graph).newEventContext(user,
                EventTypes.deletion,
                getLogMessage(logMessage))
                .commit();
        deleteIds(graph, manager, type, user);

        return 0;
    }

    private void deleteIds(FramedGraph<?> graph, GraphManager manager, EntityClass type, UserProfile user)
            throws SerializationError, ValidationError, ItemNotFound, PermissionDenied {
        CrudViews<Accessible> views = new CrudViews<>(graph, Accessible.class);
        try (CloseableIterable<Accessible> items = manager.getEntities(type, Accessible.class)) {
            for (Accessible acc : items) {
                System.out.println(acc.getId());
                views.delete(acc.getId(), graph.frame(user.asVertex(), Accessor.class));
            }
        }
    }
}