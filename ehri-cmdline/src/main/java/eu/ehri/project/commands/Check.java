package eu.ehri.project.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.List;

import static eu.ehri.project.definitions.Entities.*;

/**
 * Sanity check various parts of the graph.
 * 
 */
public class Check extends BaseCommand implements Command {

    final static String NAME = "check";

    /**
     * Constructor.
     */
    public Check() {
    }
    
	@Override
	public String getHelp() {
        String help = "perform various checks on the graph structure" +
        		"\n" + getUsage();
        return help;
	}
	
    @Override
    public String getUsage() {
		return "Usage: check [OPTIONS]";
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("q", "quick", false, "Quick checks only"));
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

        final GraphManager manager = GraphManagerFactory.getInstance(graph);
        checkPermissionScopes(graph, manager);

        return 0;
    }

    /**
     * The following types of item should ALL have a permission scope.
     *
     *  Doc unit - either a repository or another doc unit
     *  Concept - a vocabulary
     *  Repository - a country
     *  Hist agent - an auth set
     *
     * @param graph
     * @param manager
     * @throws Exception
     */
    public void checkPermissionScopes(final FramedGraph<? extends TransactionalGraph> graph,
            final GraphManager manager) throws Exception {

        List<String> types = Lists.newArrayList(DOCUMENTARY_UNIT, REPOSITORY, CVOC_CONCEPT, HISTORICAL_AGENT);

        Iterable<Vertex> vertices = graph.getVertices();
        for (Vertex vertex : vertices) {
            String type = vertex.getProperty(EntityType.TYPE_KEY);
            if (!types.contains(type)) {
                continue;
            }

            AccessibleEntity entity = graph.frame(vertex, AccessibleEntity.class);
            PermissionScope scope = entity.getPermissionScope();
            if (scope == null) {
                System.err.println("Missing scope: " + entity.getId() + " (" + vertex.getId() + ")");
            }
        }
    } 
}
