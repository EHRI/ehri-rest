package eu.ehri.project.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Import EAD from the command line...
 * 
 */
public class EadImport extends BaseCommand implements Command {

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public EadImport() {
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
    public String getHelp() {
        return "Usage: importer [OPTIONS] -user <user-id> -repo <agent-id> <neo4j-graph-dir> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        String help = "Import an EAD file into the graph database, using the specified"
                + sep + "Agent and User.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public int execWithOptions(CommandLine cmdLine) throws Exception {

        final String logMessage = "Imported from command-line";

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(
                    "Usage: importer [OPTIONS] -user <user-id> -repo <agent-id> <neo4j-graph-dir> <ead1.xml> <ead2.xml> ... <eadN.xml>");

        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabase((String) cmdLine.getArgList().get(0));
        // Get the graph and search it for the required agent...
        FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                new Neo4jGraph(graphDb));

        List<String> filePaths = new LinkedList<String>();
        for (int i = 1; i < cmdLine.getArgList().size(); i++) {
            filePaths.add((String) cmdLine.getArgList().get(i));
        }

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();

        try {

            // Find the agent
            Iterable<Agent> agents = graph.getVertices("identifier",
                    (String) cmdLine.getOptionValue("repo"), Agent.class);

            Agent agent = null;
            if (cmdLine.hasOption("createrepo")) {
                if (agents.iterator().hasNext())
                    agent = agents.iterator().next();
                else
                    agent = createAgent(graph, cmdLine.getOptionValue("repo"));
            }

            if (agent == null)
                throw new RuntimeException("No item found for agent: "
                        + cmdLine.getOptionValue("repo"));

            // Find the user
            Iterable<UserProfile> users = graph.getVertices("identifier",
                    (String) cmdLine.getOptionValue("user"), UserProfile.class);

            UserProfile user = null;
            if (cmdLine.hasOption("createuser")) {
                if (users.iterator().hasNext())
                    user = users.iterator().next();
                else
                    user = createUser(graph, cmdLine.getOptionValue("user"));
            }

            if (user == null)
                throw new RuntimeException("No item found for user: "
                        + cmdLine.getOptionValue("user"));

            EadImportManager manager = new EadImportManager(graph, agent, user);
            manager.setTolerant(cmdLine.hasOption("tolerant"));
            ImportLog log = manager.importFiles(filePaths, logMessage);

            System.out.println("Import completed. Created: " + log.getCreated()
                    + ", Updated: " + log.getUpdated());
            tx.success();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            tx.failure();
            return 1;
        } finally {
            tx.finish();
            graph.shutdown();
        }
        return 0;
    }

    private static UserProfile createUser(FramedGraph<Neo4jGraph> graph,
            String name) throws ValidationError {
        Map<String, Object> agentData = new HashMap<String, Object>();
        agentData.put("identifier", name);
        agentData.put("name", name);
        EntityBundle<UserProfile> agb = new BundleFactory<UserProfile>()
                .buildBundle(agentData, UserProfile.class);
        return new BundleDAO<UserProfile>(graph).create(agb);
    }

    private static Agent createAgent(FramedGraph<Neo4jGraph> graph, String name)
            throws ValidationError {
        Map<String, Object> agentData = new HashMap<String, Object>();
        agentData.put("identifier", name);
        agentData.put("name", name);
        EntityBundle<Agent> agb = new BundleFactory<Agent>().buildBundle(
                agentData, Agent.class);
        return new BundleDAO<Agent>(graph).create(agb);
    }
}
