/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;

import java.util.Collection;
import java.util.UUID;

/**
 * Generates a generic ID for tertiary node types.
 */
public enum GenericIdGenerator implements IdGenerator {

    INSTANCE;

    // NB: We use a time-based UUID generator here because
    // sequential UUIDs prevent index fragmentation.
    private static final TimeBasedGenerator timeBasedGenerator
            = Generators.timeBasedGenerator();

    public ListMultimap<String, String> handleIdCollision(Collection<String> scopeIds, Bundle bundle) {
        throw new RuntimeException(String.format("Index collision generating identifier for item type '%s' with data: '%s'",
                bundle.getType().getName(), bundle));
    }

    /**
     * Generates a random String.
     *
     * @param scopeIds array of scope ids
     * @param bundle   The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(Collection<String> scopeIds, Bundle bundle) {
        return getIdBase(bundle);
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        return getTimeBasedUUID().toString();
    }

    /**
     * Get a new time-based UUID.
     * @return A time based UUID.
     */
    public static UUID getTimeBasedUUID() {
        return timeBasedGenerator.generate();
    }
}
