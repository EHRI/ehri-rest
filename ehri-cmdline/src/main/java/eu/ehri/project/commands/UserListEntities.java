package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.views.Query;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Import EAD from the command line...
 * 
 */
public class UserListEntities extends BaseCommand implements Command {

    final static String NAME = "user-list";

    /**
     * Constructor.
     * 
     */
    public UserListEntities() {
    }

    @Override
    public String getHelp() {
        return "Usage: user-list [OPTIONS] <type>";
    }

    @Override
    public String getUsage() {
        return "List entities of a given type as a given user.";
    }
    
    @Override
    public void setCustomOptions() {
        options.addOption(new Option("user", true,
                "Identifier of user to list items as"));
    }

    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        UserProfile user = manager.getFrame(
                cmdLine.getOptionValue("user"), UserProfile.class);

        @SuppressWarnings("unchecked")
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                (Class<AccessibleEntity>) cls);
        for (AccessibleEntity acc : query.page(user)) {
            System.out.println(acc.getId());
        }

        return 0;
    }
}
