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

import com.tinkerpop.blueprints.TransactionalGraph;
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
 * Import Skos from the command line...
 */
public class SkosVocabularyImport extends BaseCommand implements Command {

    final static String NAME = "skos-import";

    public SkosVocabularyImport() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("scope", true, "Identifier of scope to import into, i.e. the Vocabulary"));
        options.addOption(new Option("user", true, "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false, "Don't error if a file is not valid."));
        options.addOption(new Option("log", true,
                "Log message for import action."));
    }

    @Override
    public String getHelp() {
        return "Usage: skos-import [OPTIONS] -user <user-id> -scope <vocabulary-id> <skos.rdf>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        return "Import a Skos file into the graph database, using the specified"
                + sep + "Vocabulary and User.";
    }

    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
                               CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        // at least one file specufied
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        if (!cmdLine.hasOption("scope")) {
            throw new RuntimeException("No scope (vocabulary) given for SKOS import");
        }

        String filePath = (String) cmdLine.getArgList().get(0);

        try {

            Vocabulary vocabulary = manager.getFrame(
                    cmdLine.getOptionValue("scope"), Vocabulary.class);
            // Find the user
            UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                    UserProfile.class);

            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, user, vocabulary);
            ImportLog log = importer
                    .setTolerant(cmdLine.hasOption("tolerant"))
                    .importFile(filePath, logMessage);
            log.printReport();
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
