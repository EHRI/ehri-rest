package eu.ehri.project.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.SkosCoreCvocImporter;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;

/**
 * Import Skos from the command line...
 */
public class SkosVocabularyImport extends BaseCommand implements Command {

    final static String NAME = "skos-import";

    /**
     * Constructor.
     *
     * @throws ParseException
     */
    public SkosVocabularyImport() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("createvocabulary", false,
                "Create vocabulary with the given ID"));
        options.addOption(new Option("vocabulary", true,
                "Identifier of vocabulary to import into"));
        options.addOption(new Option("createuser", false,
                "Create user with the given ID"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false,
                "Don't error if a file is not valid."));
        options.addOption(new Option("logMessage", false,
                "Log message for import action."));
    }

    @Override
    public String getHelp() {
        return "Usage: importer [OPTIONS] -user <user-id> -vocabulary <vocabulary-id> <neo4j-graph-dir> <skos.rdf>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import a Skos file into the graph database, using the specified"
                + sep + "Vocabulary and User.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
                               CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = "Imported from command-line";

        // at least one file specufied
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        //List<String> filePaths = new LinkedList<String>();
        //for (int i = 0; i < cmdLine.getArgList().size(); i++) {
        //    filePaths.add((String) cmdLine.getArgList().get(i));
        //}
        String filePath = (String) cmdLine.getArgList().get(0);

        try {

            // Find the agent
            Vocabulary vocabulary;
            try {
                vocabulary = manager.getFrame(
                        cmdLine.getOptionValue("vocabulary"), Vocabulary.class);
            } catch (ItemNotFound e) {
                if (cmdLine.hasOption("createvocabulary")) {
                    vocabulary = createVocabulary(graph, cmdLine.getOptionValue("vocabulary"));
                } else {
                    throw e;
                }
            }

            // Find the user
            UserProfile user;
            try {
                user = manager.getFrame(
                        cmdLine.getOptionValue("user"),
                        UserProfile.class);
            } catch (ItemNotFound e) {
                if (cmdLine.hasOption("createuser")) {
                    user = createUser(graph, cmdLine.getOptionValue("user"));
                } else {
                    throw e;
                }
            }

            SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, user, vocabulary); //new EadImportManager(graph, vocabulary, user);
            importer.setTolerant(cmdLine.hasOption("tolerant"));
            ImportLog log = importer.importFile(filePath, logMessage);

            System.out.println("Import completed. Created: " + log.getCreated()
                    + ", Updated: " + log.getUpdated());
            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Entry<String, String> entry : log.getErrors().entrySet()) {
                    System.out.printf(" - %-20s : %s\n", entry.getKey(),
                            entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        } finally {
            graph.shutdown();
        }
        return 0;
    }

    private static UserProfile createUser(FramedGraph<Neo4jGraph> graph,
                                          String name) throws ValidationError, IntegrityError {
        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put("identifier", name);
        userData.put("name", name);
        return new BundleDAO(graph).create(new Bundle(EntityClass.USER_PROFILE,
                userData), UserProfile.class);
    }

    private static Vocabulary createVocabulary(FramedGraph<Neo4jGraph> graph, String name)
            throws ValidationError, IntegrityError {
        System.out.println("Creating vocabulary: \"" + name + "\" ...");
        Map<String, Object> vocabularyData = new HashMap<String, Object>();
        vocabularyData.put(AccessibleEntity.IDENTIFIER_KEY, name);
        //vocabularyData.put(EntityType.ID_KEY, name);
        //vocabularyData.put("name", name);
        Bundle bundle = new Bundle(EntityClass.CVOC_VOCABULARY, vocabularyData);
        bundle = bundle.withId(name);

        Vocabulary vocabulary = new BundleDAO(graph).create(bundle, Vocabulary.class);
        System.out.println("Done Creating vocabulary");
        System.out.flush();
        return vocabulary;

    }
}
