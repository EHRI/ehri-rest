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

package eu.ehri.project.persistence.utils;

import java.util.Collection;

/**
 * Bag of miscellaneous methods!
 */
public class DataUtils {
    /**
     * Ensure a value isn't an empty array or list, which will
     * cause Neo4j to barf.
     *
     * @param value A unknown object
     * @return If the object is a sequence type, and is empty
     */
    public static boolean isEmptySequence(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        } else if (value instanceof Collection<?>) {
            return ((Collection) value).isEmpty();
        } else if (value instanceof Iterable<?>) {
            return !((Iterable)value).iterator().hasNext();
        }
        return false;
    }
}