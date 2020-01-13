/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import eu.ehri.project.models.annotations.UniqueAdjacency;

import static eu.ehri.project.definitions.Ontology.PROMOTED_BY;
import static eu.ehri.project.models.utils.JavaHandlerUtils.*;

/**
 * An entity that can be promoted and demoted.
 */
public interface Promotable extends Accessible {
    String PROMOTION_SCORE = "_promotionScore";

    @Fetch(value = PROMOTED_BY, numLevels = 1)
    @Adjacency(label = PROMOTED_BY, direction = Direction.OUT)
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

    @UniqueAdjacency(label = PROMOTED_BY)
    boolean isPromotedBy(UserProfile user);

    @JavaHandler
    boolean isPromotable();

    @JavaHandler
    int getPromotionScore();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Promotable {

        private void updatePromotionScoreCache() {
            int score = Math.toIntExact(gremlin().out(PROMOTED_BY).count()
                    - gremlin().out(Ontology.DEMOTED_BY).count());
            it().setProperty(PROMOTION_SCORE, score);
        }

        @Override
        public void addPromotion(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), PROMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
            updatePromotionScoreCache();
        }

        @Override
        public void removePromotion(UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), PROMOTED_BY);
            updatePromotionScoreCache();
        }

        @Override
        public void addDemotion(UserProfile user) {
            addUniqueRelationship(it(), user.asVertex(), Ontology.DEMOTED_BY);
            removeAllRelationships(it(), user.asVertex(), PROMOTED_BY);
            updatePromotionScoreCache();
        }

        @Override
        public void removeDemotion(UserProfile user) {
            removeAllRelationships(it(), user.asVertex(), Ontology.DEMOTED_BY);
            updatePromotionScoreCache();
        }

        @Override
        public boolean isPromoted() {
            return gremlin().out(PROMOTED_BY).count() > gremlin().out(Ontology.DEMOTED_BY).count();
        }

        @Override
        public int getPromotionScore() {
            Integer score = it().getProperty(PROMOTION_SCORE);
            return score == null
                    ? Math.toIntExact(gremlin().out(PROMOTED_BY).count()
                            - gremlin().out(Ontology.DEMOTED_BY).count())
                    : score;
        }

        @Override
        public boolean isPromotable() {
            Boolean promotable = it().getProperty(Ontology.IS_PROMOTABLE);
            return promotable != null && promotable;
        }
    }
}
