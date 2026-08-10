/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.definitions;

import com.google.common.collect.Lists;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public interface DefinitionList {

    Boolean isMultiValued();
    String name();

    ResourceBundle bundle = ResourceBundle.getBundle("eu.ehri.project.definitions.messages");

    static Map<String,String> getMap(DefinitionList[] items, Boolean multivalued) {
        return Lists.newArrayList(items).stream()
                .filter(i -> multivalued == null || i.isMultiValued() == multivalued)
                .map(i -> Lists.newArrayList(i.getName(), i.getDescription()))
                .collect(Collectors.toMap(i -> i.get(0), i -> i.get(1)));
    }

    static Map<String,String> getMap(DefinitionList[] items) {
        return getMap(items, null);
    }

    default String getResourceKey(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    default String messageKey() {
        return String.format("%s.%s", getClass().getSimpleName(), name());
    }

    default String getName() {
        return getResourceKey(messageKey());
    }

    default String getDescription() {
        return getResourceKey(String.format("%s.description", messageKey()));
    }
}
