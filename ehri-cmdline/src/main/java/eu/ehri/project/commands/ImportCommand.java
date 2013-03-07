/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.IcaAtomEadHandler;
import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.XmlImporter;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 *
 * @author linda
 */
public abstract class ImportCommand extends BaseCommand implements Command{
    private Class<? extends SaxXmlHandler> handler;
    Class<? extends AbstractImporter> importer;
    
    public ImportCommand(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer){
        this.handler = handler;
        this.importer = importer;
    }
    
    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("createrepo", false,
                "Create agent with the given ID"));
        options.addOption(new Option("repo", true,
                "Identifier of repository to import into"));
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
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = "Imported from command-line";

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        List<String> filePaths = new LinkedList<String>();
        for (int i = 0; i < cmdLine.getArgList().size(); i++) {
            filePaths.add((String) cmdLine.getArgList().get(i));
        }

        try {

            // Find the agent
            Agent agent;
            try {
                agent = manager.getFrame(cmdLine.getOptionValue("repo"), Agent.class);
            } catch (ItemNotFound e) {
                if (cmdLine.hasOption("createrepo")) {
                    agent = createAgent(graph, cmdLine.getOptionValue("repo"));
                } else {
                    throw e;
                }
            }

            // Find the user
            UserProfile user;
            try {
                user = manager.getFrame(cmdLine.getOptionValue("user"),
                        UserProfile.class);
            } catch (ItemNotFound e) {
                if (cmdLine.hasOption("createuser")) {
                    user = createUser(graph, cmdLine.getOptionValue("user"));
                } else {
                    throw e;
                }
            }

            ImportLog log = new SaxImportManager(graph, agent, user, importer, handler)
            	.importFiles(filePaths, logMessage);
            
            
            System.out.println("Import completed. Created: " + log.getCreated()
                    + ", Updated: " + log.getUpdated());
            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Map.Entry<String, String> entry : log.getErrors().entrySet()) {
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

    private static Agent createAgent(FramedGraph<Neo4jGraph> graph, String name)
            throws ValidationError, IntegrityError {
        Map<String, Object> agentData = new HashMap<String, Object>();
        agentData.put("identifier", name);
        agentData.put("name", name);
        return new BundleDAO(graph).create(new Bundle(EntityClass.AGENT,
                agentData), Agent.class);
    }
    
}
