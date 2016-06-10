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

package eu.ehri.project.api.impl;

import com.google.common.collect.Lists;
import com.tinkerpop.pipes.util.structures.Pair;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.api.impl.QueryUtils;
import org.junit.Test;

import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryUtilsTest {
    @Test
    public void testParseFilters() throws Exception {
        SortedMap<String, Pair<QueryApi.FilterPredicate, Object>> filters
                = QueryUtils.parseFilters(Lists.newArrayList("identifier__GT:c1"));
        assertTrue(filters.containsKey("identifier"));
        Pair<QueryApi.FilterPredicate, Object> pair = filters.get("identifier");
        assertEquals(QueryApi.FilterPredicate.GT, pair.getA());
        assertEquals("c1", pair.getB());
    }

    @Test
    public void testParseOrderSpecs() throws Exception {
        SortedMap<String, QueryApi.Sort> orders = QueryUtils
                .parseOrderSpecs(Lists.newArrayList("identifier__DESC", "name"));
        assertTrue(orders.containsKey("identifier"));
        QueryApi.Sort sort = orders.get("identifier");
        assertEquals(QueryApi.Sort.DESC, sort);
        QueryApi.Sort nameSort = orders.get("name");
        assertEquals(QueryApi.Sort.ASC, nameSort);
    }
}
