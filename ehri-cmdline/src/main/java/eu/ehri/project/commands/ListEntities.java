package eu.ehri.project.commands;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.views.Query;

/**
 * Import EAD from the command line...
 * 
 */
public class ListEntities extends BaseCommand implements Command {

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public ListEntities() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
    }

    @Override
    public String getHelp() {
        return "Usage: list [OPTIONS] <type>";
    }

    @Override
    public String getUsage() {
        String help = "List entities of a given type.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        String type = cmdLine.getArgs()[0];
        Map<String, Class<? extends VertexFrame>> classes = ClassUtils
                .getEntityClasses();

        Class<?> cls = classes.get(type);
        if (cls == null)
            throw new RuntimeException("Unknown entity: " + type);

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        UserProfile user = graph
                .getVertices(AccessibleEntity.IDENTIFIER_KEY,
                        (String) cmdLine.getOptionValue("user"),
                        UserProfile.class).iterator().next();

        @SuppressWarnings("unchecked")
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                (Class<AccessibleEntity>) cls);
        for (AccessibleEntity acc : query.list((Long) user.asVertex().getId())) {
            System.out.println(acc.getIdentifier());
        }
        return 0;
    }
}
