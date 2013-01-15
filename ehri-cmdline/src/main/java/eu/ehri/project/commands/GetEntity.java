package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Converter;

/**
 * Import EAD from the command line...
 * 
 */
public class GetEntity extends BaseCommand implements Command {

    final static String NAME = "get";

    /**
     * Constructor.
     * 
     * @param args
     */
    public GetEntity() {
    }

    @Override
    public String getHelp() {
        return "Usage: get [OPTIONS] <type> <identifier>";
    }

    @Override
    public String getUsage() {
        String help = "Get an entity by its identifier.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Converter converter = new Converter(graph);

        if (cmdLine.getArgList().size() < 2)
            throw new RuntimeException(getHelp());

        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        String id = cmdLine.getArgs()[1];
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        System.out.println(converter.vertexFrameToJson(manager.getFrame(id,
                type, (Class<AccessibleEntity>)cls)));
        return 0;
    }
}
