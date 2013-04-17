package eu.ehri.project.commands;

import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.neo4j.graphdb.Transaction;

import java.util.Properties;

/**
 * Add a user.
 * 
 */
public class EntityAdd extends BaseCommand implements Command {

    final static String NAME = "add";

    /**
     * Constructor.
     *
     */
    public EntityAdd() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(OptionBuilder.withArgName("property=value")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription( "Add a property with the given value" )
                .create("P"));
        options.addOption(new Option("scope", true,
                "Identifier of scope to create item in, i.e. a repository"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("c", "comment", false,
                "Log message for create action action."));
    }

    @Override
    public String getHelp() {
        return "Usage: add <type> [OPTIONS] [-Pkey=value]";
    }

    @Override
    public String getUsage() {
        String help = "Create a new entity with the given id and properties";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws eu.ehri.project.exceptions.ItemNotFound
     * @throws eu.ehri.project.exceptions.DeserializationError
     * @throws eu.ehri.project.exceptions.PermissionDenied
     * @throws eu.ehri.project.exceptions.ValidationError
     */
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        // Find the agent
        PermissionScope scope = SystemScope.getInstance();
        if (cmdLine.hasOption("scope")) {
            scope = manager.getFrame(cmdLine.getOptionValue("scope"), PermissionScope.class);
        }

        // Find the user
        UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                UserProfile.class);

        String typeName = (String) cmdLine.getArgList().get(0);
        EntityClass entityClass = EntityClass.withName(typeName);
        Properties properties = cmdLine.getOptionProperties("P");

        Bundle bundle = new Bundle(entityClass);
        for (Object prop : properties.keySet()) {
            bundle = bundle.withDataValue((String)prop, properties.getProperty((String)prop));
        }

        String id = entityClass.getIdgen().generateId(entityClass, scope, bundle);

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            LoggingCrudViews<?> view = new LoggingCrudViews(graph, entityClass.getEntityClass());
            Frame t = view.create(bundle.withId(id), user, logMessage);
            // FIXME: This is tres-ugly...
            if (!scope.equals(SystemScope.getInstance())) {
                graph.frame(t.asVertex(), AccessibleEntity.class).setPermissionScope(scope);
            }
            tx.success();
        } catch (IntegrityError e) {
            tx.failure();
            System.err.printf("A user a id: '%s' already exists\n", id);
            return 9;
        } finally {
            tx.finish();
        }

        return 0;
    }
}
