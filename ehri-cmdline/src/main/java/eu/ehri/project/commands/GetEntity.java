package eu.ehri.project.commands;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.utils.ClassUtils;
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
     * @throws ParseException
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
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 2)
            throw new RuntimeException(getHelp());

        String type = cmdLine.getArgs()[0];
        String id = cmdLine.getArgs()[1];
        Map<String, Class<? extends VertexFrame>> classes = ClassUtils
                .getEntityClasses();

        Class<?> cls = classes.get(type);
        if (cls == null)
            throw new RuntimeException("Unknown entity: " + type);

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        Index<Vertex> index = graph.getBaseGraph().getIndex(type, Vertex.class);
        CloseableIterable<Vertex> query = index.get(
                AccessibleEntity.IDENTIFIER_KEY, id);
        try {
            @SuppressWarnings("unchecked")
            AccessibleEntity item = graph.frame(query.iterator().next(),
                    (Class<AccessibleEntity>) cls);
            System.out.println(new Converter().vertexFrameToJson(item));
        } finally {
            query.close();
        }

        return 0;
    }
}
