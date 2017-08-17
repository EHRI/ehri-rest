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

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporterFactory;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Map.Entry;

/**
 * Import a single SKOS file from the command line.
 */
public class SkosVocabularyImport extends BaseCommand {

    final static String NAME = "skos-import";

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
                .longOpt("tolerant")
                .desc("Don't error if a file is not valid.")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for action.")
                .build());
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] -user <user-id> -scope <vocabulary-id> <skos.rdf>";
    }

    @Override
    public String getHelp() {
        return "Import a Skos file into the graph database, using the specified " +
                "Vocabulary and User.";
    }

    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        // at least one file specufied
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        if (!cmdLine.hasOption("scope")) {
            throw new RuntimeException("No scope (vocabulary) given for SKOS import");
        }

        String filePath = cmdLine.getArgList().get(0);

        try {

            Vocabulary vocabulary = manager.getEntity(
                    cmdLine.getOptionValue("scope"), Vocabulary.class);
            // Find the user
            UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"),
                    UserProfile.class);

            // FIXME: Casting the graph shouldn't be necessary here, but it is
            // because the import managers do transactional stuff that they
            // probably should not do.
            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, user, vocabulary);
            ImportLog log = importer
                    .setTolerant(cmdLine.hasOption("tolerant"))
                    .importFile(filePath, logMessage);
            System.out.println(log);
            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Entry<String, String> entry : log.getErrors().entrySet()) {
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
