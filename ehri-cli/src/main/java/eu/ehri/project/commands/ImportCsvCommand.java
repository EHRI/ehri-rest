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

import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.importers.ImportLog;
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
 */
public abstract class ImportCsvCommand extends BaseCommand {
    private final Class<? extends ItemImporter<?, ?>> importer;

    public ImportCsvCommand(Class<? extends ItemImporter<?, ?>> importer) {
        this.importer = importer;
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("scope")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Identifier of scope to import into, i.e. repository")
                .build());
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Identifier of user to import as")
                .build());
        options.addOption(Option.builder()
                .longOpt("lang")
                .hasArg()
                .type(String.class)
                .desc("Default language code")
                .build());
        options.addOption(Option.builder()
                .longOpt("tolerant")
                .desc("Don't error if a file is not valid.")
                .build());
        options.addOption(Option.builder()
                .longOpt("allow-updates")
                .desc("Allow the ingest process to update existing items.")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for action.")
                .build());
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        List<String> filePaths = Lists.newLinkedList();
        filePaths.addAll(cmdLine.getArgList());

        try {
            // Find the agent
            PermissionScope scope = SystemScope.getInstance();
            if (cmdLine.hasOption("scope")) {
                scope = manager.getEntity(cmdLine.getOptionValue("scope"), PermissionScope.class);
            }

            // Find the user
            UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"), UserProfile.class);
            ImportOptions options = ImportOptions.basic()
                    .withTolerant(cmdLine.hasOption("tolerant"))
                    .withUpdates(cmdLine.hasOption("allow-updates"))
                    .withDefaultLang(cmdLine.getOptionValue("lang"));

            ImportLog log = new CsvImportManager(graph, scope, user, importer, options)
                    .importFiles(filePaths, logMessage);

            System.out.println(log);
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
