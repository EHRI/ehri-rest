package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.Fetch;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Promotable extends AccessibleEntity {

    @Fetch(Ontology.PROMOTED_BY)
    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public Iterable<UserProfile> getPromotors();

    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public void addPromotion(final UserProfile user);

    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public void removePromotion(final UserProfile user);

    @JavaHandler
    public boolean isPromoted();

    @JavaHandler
    public boolean isPromotable();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Promotable {

        public boolean isPromoted() {
            return gremlin().out(Ontology.PROMOTED_BY).hasNext();
        }

        public boolean isPromotable() {
            Boolean promotable = it().getProperty(Ontology.IS_PROMOTABLE);
            return promotable != null && promotable;
        }
    }
}
