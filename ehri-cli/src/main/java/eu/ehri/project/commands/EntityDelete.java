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
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Delete a single item via the command line.
 */
public class EntityDelete extends BaseCommand {

    final static String NAME = "delete";

    public EntityDelete() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .type(String.class)
                .desc("Identifier of user to import as")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for import action.")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s --user <user> [OPTIONS] <id>", NAME);
    }

    @Override
    public String getHelp() {
        return "Delete an item by ID.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        // the first argument is the item ID, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());
        String id = cmdLine.getArgs()[0];

        String logMessage = "Deleting item " + id + " via the command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        // Find the user
        UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"),
                UserProfile.class);

        LoggingCrudViews<Accessible> api = new LoggingCrudViews<>(
                graph, Accessible.class);
        api.delete(id, user, Optional.of(logMessage));

        return 0;
    }
}