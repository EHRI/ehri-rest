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

import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.CsvImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.MapImporter;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.Map;

/**
 * Generic command for importing CSV files that uses an implementation of MapImporter
 * to manipulate the graph.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class ImportCsvCommand extends BaseCommand implements Command {
    Class<? extends MapImporter> importer;

    public ImportCsvCommand(Class<? extends MapImporter> importer) {
        this.importer = importer;
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("scope", true,
                "Identifier of scope to import into, i.e. AuthoritativeSet"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false,
                "Don't fail on individual row errors."));
        options.addOption(new Option("log", true,
                "Log message for import action."));
    }

    @Override
    public int execWithOptions(final FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        boolean tolerant = cmdLine.hasOption("tolerant");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        List<String> filePaths = Lists.newLinkedList();
        for (int i = 0; i < cmdLine.getArgList().size(); i++) {
            filePaths.add((String) cmdLine.getArgList().get(i));
        }

        try {
            // Find the agent
            PermissionScope scope = SystemScope.getInstance();
            if (cmdLine.hasOption("scope")) {
                scope = manager.getFrame(cmdLine.getOptionValue("scope"), PermissionScope.class);
            }

            // Find the user
            UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                    UserProfile.class);

            ImportLog log = new CsvImportManager(graph, scope, user, importer)
                    .setTolerant(tolerant).importFiles(filePaths, logMessage);

            log.printReport();
            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Map.Entry<String, String> entry : log.getErrors().entrySet()) {
                    System.out.printf(" - %-20s : %s%n", entry.getKey(),
                            entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }
}
