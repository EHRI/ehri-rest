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

package eu.ehri.project.utils.fixtures;

import java.io.InputStream;

/**
 * Interface for classes which handle fixture loading.
 */
public interface FixtureLoader {

    /**
     * Toggle whether or not initialization occurs before
     * loading (default: true)
     */
    FixtureLoader setInitializing(boolean toggle);

    /**
     * Load the default fixtures.
     */
    void loadTestData();

    /**
     * Load a given InputStream as test data. The stream
     * will be closed automatically.
     * @param inputStream An imput stream of fixture data
     */
    void loadTestData(InputStream inputStream);

    /**
     * Load a given file as test data.
     *
     * @param resourceNameOrPath A resource name or file path
     *                           containing fixture data.
     */
    void loadTestData(String resourceNameOrPath);
}
