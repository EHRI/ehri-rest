package eu.ehri.project.models.base;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.utils.JavaHandlerUtils;

import java.util.Collection;
import java.util.List;

/**
 * The scope of permissions granted to users. A permission scope always has an identifier.
 * 
 * @author Mike Bryant (https://github.com/mikesname)
 *
 */
public interface PermissionScope extends IdentifiableEntity {
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SCOPE, direction = Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();

    @JavaHandler
    public Iterable<PermissionScope> getPermissionScopes();

    @Adjacency(label = Ontology.HAS_PERMISSION_SCOPE, direction = Direction.IN)
    public Iterable<Frame> getContainedItems();

    @JavaHandler
    public Iterable<Frame> getAllContainedItems();

    /**
     * Get the path of the permission scope as an ordered collection of strings.
     * @return an ordered Collection of Strings that forms the 'path'.
     */
    @JavaHandler
    public Collection<String> idPath();

    abstract class Impl implements JavaHandlerContext<Vertex>, PermissionScope {
        public Iterable<Frame> getAllContainedItems() {
            return frameVertices(gremlin().as("n")
                    .in(Ontology.HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc));
        }

        public Iterable<PermissionScope> getPermissionScopes() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }

        public Collection<String> idPath() {
            // Sigh - duplication...
            List<String> pIds = Lists.reverse(gremlin().as("n")
                    .out(Ontology.HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc)
                    .transform(new PipeFunction<Vertex, String>() {
                        @Override
                        public String compute(Vertex vertex) {
                            return vertex.getProperty(Ontology.IDENTIFIER_KEY);
                        }
                    }).toList());
            pIds.add((String) it().getProperty(Ontology.IDENTIFIER_KEY));
            return pIds;
        }
    }
}
