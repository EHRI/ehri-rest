package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.Fetch;

import static eu.ehri.project.models.utils.JavaHandlerUtils.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Promotable extends AccessibleEntity {

    @Fetch(Ontology.PROMOTED_BY)
    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public Iterable<UserProfile> getUpVoters();

    @Fetch(Ontology.DEMOTED_BY)
    @Adjacency(label = Ontology.DEMOTED_BY, direction = Direction.OUT)
    public Iterable<UserProfile> getDownVoters();

    @JavaHandler
    public void promote(final UserProfile user);

    @JavaHandler
    public void demote(final UserProfile user);

    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public void removePromotion(final UserProfile user);

    @Adjacency(label = Ontology.DEMOTED_BY, direction = Direction.OUT)
    public void removeDemotion(final UserProfile user);

    @JavaHandler
    public boolean isPromoted();

    @JavaHandler
    public boolean isPromotedBy(final UserProfile user);

    @JavaHandler
    public boolean isPromotable();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Promotable {

        public void promote(final UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.PROMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
        }

        public void demote(final UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.DEMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.PROMOTED_BY);
        }

        public boolean isPromoted() {
            return gremlin().out(Ontology.PROMOTED_BY).count() > gremlin().out(Ontology.DEMOTED_BY).count();
        }

        public boolean isPromotedBy(final UserProfile user) {
            return hasRelationship(it(), user.asVertex(), Ontology.PROMOTED_BY);
        }

        public boolean isPromotable() {
            Boolean promotable = it().getProperty(Ontology.IS_PROMOTABLE);
            return promotable != null && promotable;
        }
    }
}
