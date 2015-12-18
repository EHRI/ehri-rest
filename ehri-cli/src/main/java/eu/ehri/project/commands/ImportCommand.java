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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Superclass of all import command-line tools.
 */
public abstract class ImportCommand extends BaseCommand {
    protected Class<? extends SaxXmlHandler> handler;
    protected Class<? extends AbstractImporter> importer;
    
    public ImportCommand(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer){
        this.handler = handler;
        this.importer = importer;
    }
    
    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("scope", true,
           "Identifier of scope to import into, i.e. repository"));
        options.addOption(new Option("F", "files-from", true,
           "Read list of input files from another file (or standard input, if given '-')"));
        options.addOption(new Option("user", true,
           "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false,
           "Don't error if a file is not valid."));
        options.addOption(new Option("log", true,
           "Log message for action."));
        options.addOption(new Option("properties", true,
                "Provide another property file (default depends on HandlerClass)"));
    }
    
     @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);


        List<String> filePaths = Lists.newArrayList();
        if (cmdLine.hasOption("files-from")) {
            getPathsFromFile(cmdLine.getOptionValue("files-from"), filePaths);
        } else if (!cmdLine.getArgList().isEmpty()) {
            for (int i = 0; i < cmdLine.getArgList().size(); i++) {
                filePaths.add((String) cmdLine.getArgList().get(i));
            }
        } else {
            throw new RuntimeException(getHelp());
        }

        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
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
            
            Optional<XmlImportProperties> optionalProperties = Optional.absent();
            if (cmdLine.hasOption("properties")) {
                XmlImportProperties properties = new XmlImportProperties(cmdLine.getOptionValue("properties"));
                logMessage += " Using properties file : " + cmdLine.getOptionValue("properties");
                optionalProperties = Optional.of(properties);
            }

            // FIXME: Casting the graph shouldn't be necessary here, but it is
            // because the import managers do transactional stuff that they
            // probably should not do.
            ImportLog log = new SaxImportManager(graph, scope, user,
                    importer, handler,
                    optionalProperties,
                    Lists.<ImportCallback>newArrayList())
                    .setTolerant(cmdLine.hasOption("tolerant"))
                    .importFiles(filePaths, logMessage);
            log.printReport();

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
     * @param listFile A path to a local file
     * @param filePaths An output parameter for file paths contained in
     *                  the given file.
     * @throws Exception
     */
    protected void getPathsFromFile(String listFile, List<String> filePaths) throws Exception {
        InputStream stream = listFile.contentEquals("-")
                ? System.in
                : new FileInputStream(new File(listFile));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8))) {
            String file;
            while ((file = br.readLine()) != null) {
                filePaths.add(file);
            }
        }
    }
}
