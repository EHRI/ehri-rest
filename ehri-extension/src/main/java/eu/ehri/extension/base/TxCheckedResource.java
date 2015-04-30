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

package eu.ehri.extension.base;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.TxGraph;

/**
 * A resource which handles a transactional graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface TxCheckedResource {
    /**
     * Fetch the graph associated with the resource.
     *
     * @return a transactional graph database.
     */
    public FramedGraph<? extends TxGraph> getGraph();
}
