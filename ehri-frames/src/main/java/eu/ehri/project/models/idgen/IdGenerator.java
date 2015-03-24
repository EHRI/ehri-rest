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

package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;

import java.util.Collection;
import java.util.List;

/**
 * Generate an ID given an entity type, a set of scopes, and some data.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface IdGenerator {

    /**
     * @param scopeIds  The entity's parent scope identifiers
     * @param bundle The entity's bundle data
     * @return A set of errors
     */
    public ListMultimap<String, String> handleIdCollision(Collection<String> scopeIds, Bundle bundle);

    /**
     * Generate an ID given an array of scope IDs. This can be used
     * where the scope might not yet exist.
     *
     * @param scopeIds An array of scope ids, ordered parent-to-child.
     * @param bundle   The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(Collection<String> scopeIds, Bundle bundle);

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle);
}
