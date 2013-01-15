package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Import EAD from the command line...
 * 
 */
public class ListEntities extends BaseCommand implements Command {
    
    final static String NAME = "list";

    /**
     * Constructor.
     * 
     * @param args
     */
    public ListEntities() {
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
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        GraphManager manager = GraphManagerFactory.getInstance(graph);        
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            System.out.println(manager.getId(acc));
        }
        return 0;
    }
}
