package eu.ehri.project.commands;

import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Add a country.
 * 
 */
public class CountryAdd extends BaseCommand implements Command {

    final static String NAME = "countryadd";

    /**
     * Constructor.
     * 
     */
    public CountryAdd() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("n", "name", true, "Country's full name"));
        options.addOption(new Option("c", "comment", false,
                "Log message for create action action."));
    }

    @Override
    public String getHelp() {
        return "Usage: countryadd <country-identifier> [-n <full country name>] [-c <log comment>]";
    }

    @Override
    public String getUsage() {
        String help = "Create a new country";
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
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        // Fetch the admin accessor, who's going to do the work.
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                Accessor.class);

        String countryId = (String) cmdLine.getArgList().get(0);
        String countryName = cmdLine.getOptionValue("n", countryId);
//        String[] groups = {};
//        if (cmdLine.hasOption("group")) {
//            groups = cmdLine.getOptionValues("group");
//        }

        Bundle bundle = new Bundle(EntityClass.COUNTRY,
                Maps.<String, Object> newHashMap())
                .withDataValue(Ontology.IDENTIFIER_KEY, countryId)
                .withDataValue(Ontology.NAME_KEY, countryName);

        String nodeId = EntityClass.COUNTRY.getIdgen().generateId(SystemScope.getInstance().idPath(), bundle);
        bundle = bundle.withId(nodeId);

        try {
            LoggingCrudViews<Country> view = new LoggingCrudViews<Country>(
                    graph, Country.class);
            Country newCountry = view.create(bundle, admin, getLogMessage(logMessage));
//            for (String groupId : groups) {
//                Group group = manager.getFrame(groupId, EntityClass.GROUP, Group.class);
//                group.addMember(newUser);
//            }
            graph.getBaseGraph().commit();
        } catch (IntegrityError e) {
            graph.getBaseGraph().rollback();
            System.err.printf("A country with id: '%s' already exists\n", nodeId);
            return 9;
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }

        return 0;
    }
}
