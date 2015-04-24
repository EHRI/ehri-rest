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

package eu.ehri.project.importers.properties;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class PropertiesRow {

    Map<String, String> properties;

    protected PropertiesRow() {
        properties = Maps.newHashMap();
    }

    @Override
    protected PropertiesRow clone() {
        PropertiesRow p = new PropertiesRow();
        p.properties.putAll(properties);
        return p;
    }

    /**
     * only add key value pair if value is no empty String
     *
     * @param key
     * @param value
     */
    protected PropertiesRow add(String key, String value) {
        if (!value.equals("")) {
            properties.put(key, value);
        }
        return this;
    }

    /**
     * @param key
     * @return return value if exists, null otherwise
     */
    protected String get(String key) {
        return properties.get(key);
    }
}
