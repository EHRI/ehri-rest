/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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
 * An entity that can be promoted and demoted.
 */
public interface Promotable extends AccessibleEntity {
    String PROMOTION_SCORE = "_promotionScore";

    @Fetch(value = Ontology.PROMOTED_BY, numLevels = 1)
    @Adjacency(label = Ontology.PROMOTED_BY, direction = Direction.OUT)
    Iterable<UserProfile> getPromoters();

    @Fetch(value = Ontology.DEMOTED_BY, numLevels = 1)
    @Adjacency(label = Ontology.DEMOTED_BY, direction = Direction.OUT)
    Iterable<UserProfile> getDemoters();

    @JavaHandler
    void addPromotion(UserProfile user);

    @JavaHandler
    void addDemotion(UserProfile user);

    @JavaHandler
    void removePromotion(UserProfile user);

    @JavaHandler
    void removeDemotion(UserProfile user);

    @JavaHandler
    boolean isPromoted();

    @JavaHandler
    boolean isPromotedBy(UserProfile user);

    @JavaHandler
    boolean isPromotable();

    @JavaHandler
    long getPromotionScore();

    @JavaHandler
    void updatePromotionScoreCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Promotable {

        public void updatePromotionScoreCache() {
            long score = gremlin().out(Ontology.PROMOTED_BY).count() - gremlin().out(Ontology.DEMOTED_BY).count();
            it().setProperty(PROMOTION_SCORE, score);
        }

        public void addPromotion(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.PROMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
            updatePromotionScoreCache();
        }

        public void removePromotion(UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), Ontology.PROMOTED_BY);
            updatePromotionScoreCache();
        }

        public void addDemotion(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.DEMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.PROMOTED_BY);
            updatePromotionScoreCache();
        }

        public void removeDemotion(UserProfile user) {
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

        public boolean isPromotedBy(UserProfile user) {
            return hasRelationship(it(), user.asVertex(), Ontology.PROMOTED_BY);
        }

        public boolean isPromotable() {
            Boolean promotable = it().getProperty(Ontology.IS_PROMOTABLE);
            return promotable != null && promotable;
        }
    }
}
