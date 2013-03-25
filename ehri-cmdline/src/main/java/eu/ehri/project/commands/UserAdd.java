package eu.ehri.project.commands;

import java.util.Map;

import eu.ehri.project.models.base.NamedEntity;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;

/**
 * Add a user.
 * 
 */
public class UserAdd extends BaseCommand implements Command {

    final static String NAME = "useradd";

    /**
     * Constructor.
     * 
     */
    public UserAdd() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("group", true,
                "A group to add the new user to"));
        options.addOption(new Option("n", "name", true, "User's full name"));
        options.addOption(new Option("c", "comment", false,
                "Log message for create action action."));
    }

    @Override
    public String getHelp() {
        return "Usage: useradd [OPTIONS] <user-identifier>";
    }

    @Override
    public String getUsage() {
        String help = "Create a new user, and optionally add them to a group";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @throws ItemNotFound
     * @throws DeserializationError 
     * @throws PermissionDenied 
     * @throws ValidationError 
     */
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        // Fetch the admin accessor, who's going to do the work.
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                Accessor.class);

        String userId = (String) cmdLine.getArgList().get(0);
        String userName = cmdLine.getOptionValue("n", userId);
        String[] groups = {};
        if (cmdLine.hasOption("group")) {
            groups = cmdLine.getOptionValues("group");
        }

        Map<String, Object> data = ImmutableMap.<String, Object> of(
                AccessibleEntity.IDENTIFIER_KEY, userId, NamedEntity.NAME,
                userName);
        Bundle bundle = new Bundle(EntityClass.USER_PROFILE,
                Maps.<String, Object> newHashMap())
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY, userId)
                .withDataValue(NamedEntity.NAME, userName);
        String nodeId = AccessibleEntityIdGenerator.INSTANCE.generateId(
                EntityClass.USER_PROFILE, SystemScope.getInstance(), bundle);
        bundle = bundle.withId(nodeId);

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            LoggingCrudViews<UserProfile> view = new LoggingCrudViews<UserProfile>(
                    graph, UserProfile.class);
            UserProfile newUser = view.create(bundle, admin, logMessage);
            for (String groupId : groups) {
                Group group = manager.getFrame(groupId, EntityClass.GROUP, Group.class);
                group.addMember(newUser);
            }
            tx.success();
        } catch (IntegrityError e) {
            tx.failure();
            System.err.printf("A user a id: '%s' already exists\n", nodeId);
            return 9;
        } finally {
            tx.finish();
        }

        return 0;
    }
}
