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

package eu.ehri.project.exceptions;

/**
 * Represents a failure to find an item in the graph based
 * on its ID value or an arbitrary key/value pair.
 */
public class ItemNotFound extends Exception {
    private static final long serialVersionUID = -3562833443079995695L;

    private String key;
    private String value;

    public ItemNotFound(String id) {
        super(String.format("Item with id '%s' not found", id));
        this.key = "id";
        this.value = id;
    }

    public ItemNotFound(String key, String value) {
        super(String.format("Item with key '%s'='%s' not found", key, value));
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
