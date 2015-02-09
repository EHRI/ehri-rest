package eu.ehri.project.tools;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Serializer;

import java.util.Collection;
import java.util.List;

/**
 * Util class for re-generating the IDs for a given
 * set of items.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IdRegenerator {
    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;
    private final Serializer depSerializer;
    private final boolean dryrun;

    public IdRegenerator(FramedGraph<? extends TransactionalGraph> graph) {
        this(graph, true);
    }

    private IdRegenerator(FramedGraph<? extends TransactionalGraph> graph, boolean dryrun) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.depSerializer = new Serializer.Builder(graph).dependentOnly().build();
        this.dryrun = dryrun;
    }

    public List<List<String>> reGenerateIds(PermissionScope scope, Iterable<? extends Frame> items) {
        List<List<String>> remaps = Lists.newArrayList();
        for (Frame item: items) {
            Optional<List<String>> optionalRemap = reGenerateId(scope, item);
            if (optionalRemap.isPresent()) {
                remaps.add(optionalRemap.get());
            }
        }
        return remaps;
    }

    public List<List<String>> reGenerateIds(Iterable<? extends AccessibleEntity> items) {
        List<List<String>> remaps = Lists.newArrayList();
        for (AccessibleEntity item: items) {
            Optional<List<String>> optionalRemap = reGenerateId(item);
            if (optionalRemap.isPresent()) {
                remaps.add(optionalRemap.get());
            }
        }
        return remaps;
    }

    public Optional<List<String>> reGenerateId(AccessibleEntity item) {
        return reGenerateId(item.getPermissionScope(), item);
    }

    public Optional<List<String>> reGenerateId(PermissionScope permissionScope, Frame item) {
        String currentId = item.getId();
        Collection<String> idChain = Lists.newArrayList();
        if (permissionScope != null && !permissionScope.equals(SystemScope.getInstance())) {
            idChain.addAll(permissionScope.idPath());
        }

        EntityClass entityClass = manager.getEntityClass(item);
        try {
            String newId = entityClass.getIdgen().generateId(idChain, depSerializer.vertexFrameToBundle(item));
            if (!newId.equals(currentId)) {
                if (!dryrun) {
                    manager.renameVertex(item.asVertex(), currentId, newId);
                }
                List<String> remap = Lists.newArrayList(currentId, newId);
                return Optional.of(remap);
            } else {
                return Optional.absent();
            }
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtain a re-generator that will actually perform the rename
     * step.
     *
     * @param doIt whether to actually commit changes
     *
     * @return a new, more dangerous, re-generator
     */
    public IdRegenerator withActualRename(boolean doIt) {
        return new IdRegenerator(graph, !doIt);
    }
}
