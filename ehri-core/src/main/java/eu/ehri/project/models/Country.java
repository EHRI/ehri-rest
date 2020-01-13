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

package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.annotations.UniqueAdjacency;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Versioned;

/**
 * Frame class representing a country. It's identifier should
 * be represented by an ISO3166 Alpha 2 code, lower cased.
 */
@EntityType(EntityClass.COUNTRY)
public interface Country extends PermissionScope, ItemHolder, Versioned, Annotatable {

    /**
     * Alias function for fetching the country code identifier.
     *
     * @return The country code
     */
    @Mandatory
    @Property(Ontology.IDENTIFIER_KEY)
    String getCode();

    /**
     * Fetch a count of the number of repositories in this country.
     *
     * @return the repository count
     */
    @Meta(CHILD_COUNT)
    @UniqueAdjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN)
    int countChildren();

    /**
     * Fetch all repositories in this country.
     *
     * @return an iterable of repository frames
     */
    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN)
    Iterable<Repository> getRepositories();

    @JavaHandler
    Iterable<DocumentaryUnit> getTopLevelDocumentaryUnits();

    /**
     * Add a repository to this country.
     *
     * @param repository a repository frame
     */
    @UniqueAdjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN, single = true)
    void addRepository(Repository repository);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Country {
        @Override
        public Iterable<DocumentaryUnit> getTopLevelDocumentaryUnits() {
            return frameVertices(gremlin().in(Ontology.REPOSITORY_HAS_COUNTRY)
                    .in(Ontology.DOC_HELD_BY_REPOSITORY), DocumentaryUnit.class);
        }
    }
}
