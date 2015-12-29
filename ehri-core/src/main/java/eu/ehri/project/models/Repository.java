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

package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.utils.JavaHandlerUtils;


/**
 * A frame class for graph nodes representing repository
 * items.
 */
@EntityType(EntityClass.REPOSITORY)
public interface Repository extends DescribedEntity,
        ItemHolder, Watchable {

    /**
     * Count the number of top-level documentary unit items within
     * this repository.
     *
     * @return the number of top-level items
     */
    @Meta(CHILD_COUNT)
    @JavaHandler
    long getChildCount();

    /**
     * Fetch all top-level documentary unit items within this
     * repository.
     *
     * @return an iterable of top-level items
     */
    @Adjacency(label = Ontology.DOC_HELD_BY_REPOSITORY, direction = Direction.IN)
    Iterable<DocumentaryUnit> getCollections();

    /**
     * Fetch items at <b>all</b> levels (including children of top-level
     * items and their children, recursively.)
     *
     * @return an iterable of documentary unit items
     */
    @JavaHandler
    Iterable<DocumentaryUnit> getAllCollections();

    /**
     * Add a documentary unit as a top-level item in this
     * repository.
     *
     * @param unit a documentary unit item
     */
    @JavaHandler
    void addCollection(DocumentaryUnit unit);

    /**
     * Fetch the country in which this repository resides.
     *
     * @return a country frame
     */
    @Fetch(Ontology.REPOSITORY_HAS_COUNTRY)
    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.OUT)
    Country getCountry();

    /**
     * The the country in which this repository resides.
     *
     * @param country a country frame
     */
    @JavaHandler
    void setCountry(Country country);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Repository {

        public long getChildCount() {
            return gremlin().inE(Ontology.DOC_HELD_BY_REPOSITORY).count();
        }

        public void addCollection(DocumentaryUnit unit) {
            JavaHandlerUtils.addSingleRelationship(unit.asVertex(), it(),
                    Ontology.DOC_HELD_BY_REPOSITORY);
        }

        public void setCountry(Country country) {
            country.addRepository(frame(it(), Repository.class));
        }

        public Iterable<DocumentaryUnit> getAllCollections() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY)
                    .cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }
    }
}
