package eu.ehri.project.commands;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
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
        for (AccessibleEntity acc : query.list(null, null, user)) {
            System.out.println(acc.getIdentifier());
        }
        
        return 0;
    }
}
