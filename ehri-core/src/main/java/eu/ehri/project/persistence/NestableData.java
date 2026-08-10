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

package eu.ehri.project.persistence;

import com.google.common.collect.Multimap;

import java.util.List;

/**
 * A data structure representation of a property graph.
 *
 * @param <N> the concrete type
 */
public interface NestableData<N> {

    Multimap<String, N> getRelations();

    List<N> getRelations(String relation);

    boolean hasRelations(String relation);

    <T> T getDataValue(String key);

    N withRelations(Multimap<String, N> relations);

    N withRelations(String name, List<N> relations);

    N withRelation(String name, N value);

    N replaceRelations(Multimap<String, N> relations);

    N withDataValue(String key, Object value);

    N removeDataValue(String key);
}
