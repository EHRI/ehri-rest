/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.api.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.tinkerpop.pipes.util.structures.Pair;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

/**
 * Utilities for parsing filter and order specifiers
 * for the ad-hoc (and out-dated) query syntax. This
 * is mainly used internally for testing and not exposed
 * externally.
 */
class QueryUtils {

    private static final Splitter predicateSplitter = Splitter.on("__").limit(2);
    private static final Splitter valueSplitter = Splitter.on(":").limit(2);

    /**
     * Parse a list of string filter specifications.
     *
     * @param filterList A list of filter spec strings
     * @return A map of filter specs
     */
    static SortedMap<String, Pair<QueryApiImpl.FilterPredicate, Object>> parseFilters(Collection<String>
            filterList) {
        ImmutableSortedMap.Builder<String, Pair<QueryApiImpl.FilterPredicate, Object>> builder =
                new ImmutableSortedMap.Builder<>(Ordering.natural());
        for (String filter : filterList) {
            List<String> kv = valueSplitter.splitToList(filter);
            if (kv.size() == 2) {
                String ppred = kv.get(0);
                String value = kv.get(1);
                List<String> pp = predicateSplitter.splitToList(ppred);
                if (pp.size() == 1) {
                    builder.put(pp.get(0), new Pair<>(
                            QueryApiImpl.FilterPredicate.EQUALS, value));
                } else if (pp.size() > 1) {
                    builder.put(pp.get(0), new Pair<>(
                            QueryApiImpl.FilterPredicate.valueOf(pp.get(1)), value));
                }
            }
        }
        return builder.build();
    }

    /**
     * Parse a list of sort specifications.
     *
     * @param orderSpecs A list of order spec strings
     * @return A map of order specs
     */
    static SortedMap<String, QueryApiImpl.Sort> parseOrderSpecs(Collection<String> orderSpecs) {
        ImmutableSortedMap.Builder<String, QueryApiImpl.Sort> builder =
                new ImmutableSortedMap.Builder<>(Ordering.natural());
        for (String spec : orderSpecs) {
            List<String> od = predicateSplitter.splitToList(spec);
            if (od.size() == 1) {
                builder.put(od.get(0), QueryApiImpl.Sort.ASC);
            } else if (od.size() > 1) {
                builder.put(od.get(0), QueryApiImpl.Sort.valueOf(od.get(1)));
            }
        }
        return builder.build();
    }
}
