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

package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.UniqueAdjacency;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.Named;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * A frame class representing a item that holds other
 * <i>authoritative</i> items, such as concepts and
 * historical agents.
 */
@EntityType(EntityClass.AUTHORITATIVE_SET)
public interface AuthoritativeSet extends Accessible,
        PermissionScope, ItemHolder, Named {

    @UniqueAdjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    int countChildren();

    /**
     * Fetch all items within this set.
     *
     * @return an iterable of authoritative items
     */
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    Iterable<AuthoritativeItem> getAuthoritativeItems();

    /**
     * Add an authoritative item to this set.
     *
     * @param item an authoritative item frame
     */
    @UniqueAdjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN, single = true)
    void addItem(AuthoritativeItem item);
}
