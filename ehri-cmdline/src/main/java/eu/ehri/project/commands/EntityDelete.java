package eu.ehri.project.commands;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.CrudViews;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Delete an entire class of stuff via the command line.
 */
public class EntityDelete extends BaseCommand implements Command {

    final static String NAME = "delete";

    /**
     * Constructor.
     */
    public EntityDelete() {
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("log", true,
                "Log message for import action."));
    }

    @Override
    public String getHelp() {
        return String.format("Usage: %s --user <user> [OPTIONS] <id>", NAME);
    }

    @Override
    public String getUsage() {
        return "Delete an item by ID.";
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        // the first argument is the entity type, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());
        String id = cmdLine.getArgs()[1];

        String logMessage = "Deleting item " + id + " via the command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        // Find the user
        UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                UserProfile.class);

        try {
            LoggingCrudViews<AccessibleEntity> api = new LoggingCrudViews<AccessibleEntity>(
                    graph, AccessibleEntity.class);
            api.delete(id, user, Optional.of(logMessage));
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }

        return 0;
    }
}