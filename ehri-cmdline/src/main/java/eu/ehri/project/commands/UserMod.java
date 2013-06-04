package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.ActionManager.EventContext;

/**
 * Add a user.
 * 
 */
public class UserMod extends BaseCommand implements Command {

    final static String NAME = "usermod";

    /**
     * Constructor.
     * 
     */
    public UserMod() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("group", true,
                "A group to add the new user to"));
        options.addOption(new Option("c", "comment", false,
                "Log message for create action action."));
    }

    @Override
    public String getHelp() {
        return "Usage: usermod [OPTIONS] <user-identifier>";
    }

    @Override
    public String getUsage() {
        String help = "Create a new user, and optionally add them to a group";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param graph
     * @param cmdLine
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws PermissionDenied
     * @throws ValidationError
     */
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError,
            PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = cmdLine.getOptionValue("c",
                "Adding user to groups");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        // Fetch the admin accessor, who's going to do the work.
        Actioner admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                Actioner.class);

        String userId = (String) cmdLine.getArgList().get(0);

        String[] groups = {};
        if (cmdLine.hasOption("group")) {
            groups = cmdLine.getOptionValues("group");
        }

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            UserProfile user = manager.getFrame(userId,
                    EntityClass.USER_PROFILE, UserProfile.class);

            EventContext actionCtx = new ActionManager(graph).logEvent(
                    user, admin, ActionManager.ActionType.updateItem,
                    getLogMessage(logMessage));

            for (String groupId : groups) {
                Group group = manager.getFrame(groupId, EntityClass.GROUP,
                        Group.class);
                group.addMember(user);
                actionCtx.addSubjects(group);
            }
            tx.success();
        } finally {
            tx.finish();
        }

        return 0;
    }
}
