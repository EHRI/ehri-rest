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

package eu.ehri.project.importers.properties;

import java.util.Set;

/**
 * defines the interface for all import mappings
 */
public interface ImportProperties {

    /**
     * Get the value for the specified key.
     *
     * @param key a property key
     * @return a property value, or null
     */
    String getProperty(String key);

    /**
     * See whether a value exists in the properties file.
     *
     * @param value a value to look for
     * @return true when found, false when it doesn't exist in the file
     */
    boolean containsPropertyValue(String value);

    /**
     * @return the right-hand side of the properties file
     */
    Set<String> getAllNonAttributeValues();

    boolean containsProperty(String key);

    boolean hasAttributeProperty(String key);

    /**
     * Get the value for the given attribute name.
     *
     * @param key a property key
     * @return a property attribute value, or null
     */
    String getAttributeProperty(String key);
}
