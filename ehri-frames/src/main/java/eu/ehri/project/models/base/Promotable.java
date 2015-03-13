package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
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
    public static final String PROMOTION_SCORE = "_promotionScore";

    @Fetch(value = Ontology.PROMOTED_BY, numLevels = 1)
    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    public Iterable<UserProfile> getPromoters();

    @Fetch(value = Ontology.DEMOTED_BY, numLevels = 1)
    @Adjacency(label = Ontology.DEMOTED_BY, direction = Direction.OUT)
    public Iterable<UserProfile> getDemoters();

    @JavaHandler
    public void addPromotion(final UserProfile user);

    @JavaHandler
    public void addDemotion(final UserProfile user);

    @JavaHandler
    public void removePromotion(final UserProfile user);

    @JavaHandler
    public void removeDemotion(final UserProfile user);

    @JavaHandler
    public boolean isPromoted();

    @JavaHandler
    public boolean isPromotedBy(final UserProfile user);

    @JavaHandler
    public boolean isPromotable();

    @JavaHandler
    public long getPromotionScore();

    @JavaHandler
    public void updatePromotionScoreCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Promotable {

        public void updatePromotionScoreCache() {
            long score = gremlin().out(Ontology.PROMOTED_BY).count() - gremlin().out(Ontology.DEMOTED_BY).count();
            it().setProperty(PROMOTION_SCORE, score);
        }

        public void addPromotion(final UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.PROMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
            updatePromotionScoreCache();
        }

        public void removePromotion(final UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), Ontology.PROMOTED_BY);
            updatePromotionScoreCache();
        }

        public void addDemotion(final UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.DEMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.PROMOTED_BY);
            updatePromotionScoreCache();
        }

        public void removeDemotion(final UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
            updatePromotionScoreCache();
        }

        public boolean isPromoted() {
            return gremlin().out(Ontology.PROMOTED_BY).count() > gremlin().out(Ontology.DEMOTED_BY).count();
        }

        public long getPromotionScore() {
            Long score = it().getProperty(PROMOTION_SCORE);
            return score == null
                    ?gremlin().out(Ontology.PROMOTED_BY).count() - gremlin().out(Ontology.DEMOTED_BY).count()
                    : score;
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
