package eu.ehri.project.commands;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
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
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.views.impl.CrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.neo4j.graphdb.Transaction;

/**
 * Delete an entire class of stuff via the command line.
 */
public class DeleteEntities extends BaseCommand implements Command {

    final static String NAME = "delete";

    /**
     * Constructor.
     */
    public DeleteEntities() {
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
        return "Usage: delete [OPTIONS] <type>";
    }

    @Override
    public String getUsage() {
        String help = "Delete entities of a given type.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {

        // the first argument is the entity type, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        String logMessage = "Deleting items of type " + type.toString() + " via the command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        // Find the user
        UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                UserProfile.class);

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            new ActionManager(graph).logEvent(user,
                    EventTypes.deletion,
                    getLogMessage(logMessage));
            deleteIds(graph, manager, type, user);
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }

        return 0;
    }

    /**
     * Output node IDs only.
     *
     * @param graph
     * @param manager
     * @param type
     * @param user
     */
    private void deleteIds(FramedGraph<Neo4jGraph> graph, GraphManager manager, EntityClass type, UserProfile user)
            throws SerializationError, ValidationError, ItemNotFound, PermissionDenied {
        CrudViews<AccessibleEntity> views = new CrudViews<AccessibleEntity>(graph, AccessibleEntity.class);
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            System.out.println(acc.getId());
            views.delete(acc, graph.frame(user.asVertex(), Accessor.class));
        }
    }
}