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

package eu.ehri.project.importers;

import eu.ehri.project.persistence.Bundle;

import java.util.Collection;
import java.util.List;

/**
 * Implementing classes do things after an item was imported and created
 * a mutation ('created', 'updated', 'unchanged').
 */
public interface PreImportCallback {
    Bundle preImport(Collection<String> idPath, Bundle data);

    /**
     * Generate a new bundle by running it through a set of callbacks.
     *
     * @param idPath IDs of scope items
     * @param data   the Bundle instance
     * @param todo   a list of PreImportCallbacks
     * @return a new Bundle
     */
    static Bundle handlePreCallbacks(Collection<String> idPath, Bundle data, List<PreImportCallback> todo) {
        if (todo.isEmpty()) {
            return data;
        } else {
            PreImportCallback callback = todo.get(0);
            return handlePreCallbacks(idPath, callback.preImport(idPath, data), todo.subList(1, todo.size()));
        }
    }
}
