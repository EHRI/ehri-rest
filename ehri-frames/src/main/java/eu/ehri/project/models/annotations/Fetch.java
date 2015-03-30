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

package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the entity pointed to by the framed Adjacency as one that should
 * typically be fetched and displayed along with the master object.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fetch {
    public static final int DEFAULT_TRAVERSALS = 10;

    /**
     * Only serialize this relationship if the depth of traversal
     * is below this value. The default value is {@value #DEFAULT_TRAVERSALS}.
     *
     * @return the level below which this relationship should be traversed
     */
    int ifBelowLevel() default DEFAULT_TRAVERSALS;

    /**
     * Only traverse this relationship at the specified level. Ignored
     * if the level is < 0.
     *
     * @return the level at which this relation should be traversed
     */
    int ifLevel() default -1;

    /**
     * The number of levels this relationship should traverse
     * from the current item. i.e. if this item is at level N
     * the serialization of it's relations should stop at level
     * N + {@code Fetch.numLevels()}.
     */
    int numLevels() default -1;

    /**
     * Only serialize this relation when not serializing in
     * 'lite' mode.
     * @return to serialize or not
     */
    boolean whenNotLite() default false;

    /**
     * Always serialize in full mode, regardless of whether
     * lite serialization is enabled.
     *
     * @return override lite mode serialization
     */
    boolean full() default false;

    /**
     * The name of the relation when serialized.
     *
     * @return the relation name
     */
    String value();
}
