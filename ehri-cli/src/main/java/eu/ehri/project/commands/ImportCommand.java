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

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Superclass of all import command-line tools.
 */
public abstract class ImportCommand extends BaseCommand {
    private final Class<? extends SaxXmlHandler> handler;
    private final Class<? extends ItemImporter<?, ?>> importer;

    public ImportCommand(Class<? extends SaxXmlHandler> handler, Class<? extends ItemImporter<?, ?>> importer) {
        this.handler = handler;
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
        options.addOption(Option.builder("F")
                .longOpt("files-from")
                .hasArg()
                .type(String.class)
                .desc("Read list of input files from another file (or standard input, if given '-')")
                .build());
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Identifier of user to import as")
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
                .longOpt("lang")
                .hasArg()
                .type(String.class)
                .desc("Default language code")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for action.")
                .build());
        options.addOption(Option.builder()
                .longOpt("properties")
                .hasArg()
                .type(String.class)
                .desc("Provide another property file (default depends on HandlerClass)")
                .build());
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);


        List<String> filePaths = Lists.newArrayList();
        if (cmdLine.hasOption("files-from")) {
            getPathsFromFile(cmdLine.getOptionValue("files-from"), filePaths);
        } else if (!cmdLine.getArgList().isEmpty()) {
            filePaths.addAll(cmdLine.getArgList());
        } else {
            throw new RuntimeException(getUsage());
        }

        String logMessage = cmdLine.hasOption("log")
                ? cmdLine.getOptionValue("log")
                : "Imported from command-line";

        String lang = cmdLine.hasOption("lang")
                ? cmdLine.getOptionValue("lang")
                : "eng";

        try {
            // Find the agent
            PermissionScope scope = SystemScope.getInstance();
            if (cmdLine.hasOption("scope")) {
                scope = manager.getEntity(cmdLine.getOptionValue("scope"), PermissionScope.class);
            }

            // Find the user
            UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"),
                    UserProfile.class);

            if (cmdLine.hasOption("properties")) {
                logMessage += " Using properties file : " + cmdLine.getOptionValue("properties");
            }

            ImportOptions options = ImportOptions.basic()
                    .withProperties(cmdLine.getOptionValue("properties"))
                    .withUpdates(cmdLine.hasOption("allow-updates"))
                    .withTolerant(cmdLine.hasOption("tolerant"))
                    .withDefaultLang(lang);

            ImportLog log = new SaxImportManager(graph, scope, user,
                    importer,
                    handler,
                    options,
                    Lists.newArrayList())
                    .importFiles(filePaths, logMessage);
            System.out.println(log);

            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Map.Entry<String, String> entry : log.getErrors().entrySet()) {
                    System.out.printf(" - %-20s : %s%n", entry.getKey(),
                            entry.getValue());
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    /**
     * Read a set of file paths from an input, either a file or standard in
     * if given the path '-'.
     *
     * @param listFile  A path to a local file
     * @param filePaths An output parameter for file paths contained in
     *                  the given file.
     */
    private void getPathsFromFile(String listFile, List<String> filePaths) throws Exception {
        InputStream stream = listFile.contentEquals("-")
                ? System.in
                : Files.newInputStream(Paths.get(listFile));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8))) {
            String file;
            while ((file = br.readLine()) != null) {
                filePaths.add(file);
            }
        }
    }
}
