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

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.fixtures.impl.YamlFixtureLoader;

/**
 * Factory class for concealing details of Fixture loading
 * implementation.
 */
public class FixtureLoaderFactory {
    /**
     * Get an instance of a fixture loader for the given class.
     * 
     * @param graph the framed graph
     * @return a new fixture loader
     */
    public static FixtureLoader getInstance(FramedGraph<?> graph) {
        return new YamlFixtureLoader(graph);
    }

    /**
     * Get an instance of a fixture loader for the given class, specifying
     * whether or not to initialize the graph before loading.
     *
     * @param graph the new graph
     * @param initialize whether or not to initialize the graph
     * @return a new fixture loader
     */
    public static FixtureLoader getInstance(FramedGraph<?> graph, boolean initialize) {
        return new YamlFixtureLoader(graph, initialize);
    }
}
