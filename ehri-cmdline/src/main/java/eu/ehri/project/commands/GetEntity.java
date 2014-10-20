package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistence.Serializer;
import org.apache.commons.cli.CommandLine;

/**
 * Import EAD from the command line...
 * 
 */
public class GetEntity extends BaseCommand implements Command {

    final static String NAME = "get";

    /**
     * Constructor.
     */
    public GetEntity() {
    }

    @Override
    public String getHelp() {
        return "Usage: get [OPTIONS] <type> <identifier>";
    }

    @Override
    public String getUsage() {
        return "Get an entity by its identifier.";
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);

        if (cmdLine.getArgList().size() < 2)
            throw new RuntimeException(getHelp());

        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        String id = cmdLine.getArgs()[1];
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        System.out.println(serializer.vertexFrameToJson(manager.getFrame(id,
                type, (Class<AccessibleEntity>)cls)));
        return 0;
    }
}
