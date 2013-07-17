package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.utils.JavaHandlerUtils;

public interface Accessor extends IdentifiableEntity {

    @JavaHandler
    public boolean isAdmin();

    @JavaHandler
    public boolean isAnonymous();

    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Accessor> getParents();

    @JavaHandler
    public Iterable<Accessor> getAllParents();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SUBJECT, direction=Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SUBJECT, direction=Direction.IN)
    public void addPermissionGrant(final PermissionGrant grant);

    abstract class Impl implements JavaHandlerContext<Vertex>, Accessor {

        public boolean isAdmin() {
            return it().getProperty(Ontology.IDENTIFIER_KEY).equals(Group.ADMIN_GROUP_IDENTIFIER);
        }

        public boolean isAnonymous() {
            return false;
        }

        public Iterable<Accessor> getAllParents() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
