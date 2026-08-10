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

package eu.ehri.project.models.annotations;

import com.tinkerpop.blueprints.Direction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When annotating a setter method adds an adjacency
 * between source and target if it does not already exist.
 *
 * On a getter method retrieves the target.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UniqueAdjacency {
    /**
     * The label of the edges making the adjacency between the vertices.
     *
     * @return the edge label
     */
    String label();

    /**
     * The edge direction of the adjacency.
     *
     * @return the direction of the edges composing the adjacency
     */
    Direction direction() default Direction.OUT;

    /**
     * If true, specifies that the source can only have one
     * relationship of this type.
     *
     * @return the direction of the edges composing the adjacency
     */
    boolean single() default false;
}
