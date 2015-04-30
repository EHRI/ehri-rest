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

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Delete a single item via the command line.
 * 
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class EntityDelete extends BaseCommand implements Command {

    final static String NAME = "delete";

    public EntityDelete() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("user", true,
           "Identifier of user to import as"));
        options.addOption(new Option("log", true,
                "Log message for import action."));
    }

    @Override
    public String getHelp() {
        return String.format("Usage: %s --user <user> [OPTIONS] <id>", NAME);
    }

    @Override
    public String getUsage() {
        return "Delete an item by ID.";
    }

    @Override
    public int execWithOptions(final FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        // the first argument is the item ID, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());
        String id = cmdLine.getArgs()[0];

        String logMessage = "Deleting item " + id + " via the command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        // Find the user
        UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                UserProfile.class);

        LoggingCrudViews<AccessibleEntity> api = new LoggingCrudViews<>(
                graph, AccessibleEntity.class);
        api.delete(id, user, Optional.of(logMessage));

        return 0;
    }
}