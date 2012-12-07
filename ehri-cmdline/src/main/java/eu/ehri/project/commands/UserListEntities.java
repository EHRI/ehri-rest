package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityEnumTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.views.impl.Query;

/**
 * Import EAD from the command line...
 * 
 */
public class UserListEntities extends BaseCommand implements Command {

    final static String NAME = "user-list";

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public UserListEntities() {
    }

    @Override
    public String getHelp() {
        return "Usage: user-list [OPTIONS] <type>";
    }

    @Override
    public String getUsage() {
        String help = "List entities of a given type as a given user.";
        return help;
    }
    
    @Override
    public void setCustomOptions() {
        options.addOption(new Option("user", true,
                "Identifier of user to list items as"));
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        EntityEnumTypes type = EntityEnumTypes.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        UserProfile user = manager.getFrame(
                (String) cmdLine.getOptionValue("user"), UserProfile.class);

        @SuppressWarnings("unchecked")
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                (Class<AccessibleEntity>) cls);
        for (AccessibleEntity acc : query.list(user)) {
            System.out.println(manager.getId(acc));
        }

        return 0;
    }
}
