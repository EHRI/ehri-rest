package eu.ehri.project.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.idgen.IdGeneratorUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.List;

import static eu.ehri.project.models.EntityClass.*;

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
        return "perform various checks on the graph structure" +
        		"\n" + getUsage();
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
     * @param graph The graph
     * @param manager The graph manager
     * @throws Exception
     */
    public void checkPermissionScopes(final FramedGraph<? extends TransactionalGraph> graph,
            final GraphManager manager) throws Exception {

        List<EntityClass> types = Lists.newArrayList(DOCUMENTARY_UNIT, REPOSITORY, CVOC_CONCEPT, HISTORICAL_AGENT);

        for (EntityClass entityClass : types) {
            CloseableIterable<? extends Frame> items
                    = manager.getFrames(entityClass, entityClass.getEntityClass());
            try {
                for (Frame item : items) {
                    AccessibleEntity entity = graph.frame(item.asVertex(), AccessibleEntity.class);
                    PermissionScope scope = entity.getPermissionScope();
                    if (scope == null) {
                        System.err.println("Missing scope: " + entity.getId() + " (" + entity.asVertex().getId() + ")");
                    } else {
                        switch (manager.getEntityClass(item)) {
                            case DOCUMENTARY_UNIT:
                                checkIdGeneration(graph.frame(item.asVertex(), DocumentaryUnit.class), scope);
                                break;
                            case REPOSITORY:
                                checkIdGeneration(graph.frame(item.asVertex(), Repository.class), scope);
                                break;
                            default:
                        }
                    }
                }
            } finally {
                items.close();
            }
        }
    }

    private void checkIdGeneration(IdentifiableEntity doc, PermissionScope scope) {
        if (scope != null) {
            String ident = doc.getIdentifier();
            List<String> path = Lists.newArrayList(Iterables.concat(scope.idPath(), Lists.newArrayList(ident)));
            String finalId = IdGeneratorUtils.joinPath(path);
            if (!finalId.equals(doc.getId())) {
                System.err.println(String.format("Generated ID does not match scopes: '%s' -> %s + %s",
                        doc.getId(), path, ident));
            }
        }
    }
}
