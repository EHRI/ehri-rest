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

package eu.ehri.project.graphql;

/**
 * Names of query arguments and connection fields used throughout the schema,
 * plus the default/max page sizes applied to unbounded list queries.
 */
final class GraphQLConstants {

    private GraphQLConstants() {
    }

    // Query argument names
    static final String SLICE_PARAM = "at";
    static final String FIRST_PARAM = "first";
    static final String FROM_PARAM = "from";
    static final String AFTER_PARAM = "after";
    static final String ALL_PARAM = "all";
    static final String LANG_PARAM = "lang";
    static final String TOP_LEVEL_PARAM = "topLevel";

    // Connection/edge field names
    static final String HAS_PREVIOUS_PAGE = "hasPreviousPage";
    static final String HAS_NEXT_PAGE = "hasNextPage";
    static final String PAGE_INFO = "pageInfo";
    static final String ITEMS = "items";
    static final String EDGES = "edges";
    static final String NODE = "node";
    static final String CURSOR = "cursor";
    static final String NEXT_PAGE = "nextPage";
    static final String PREVIOUS_PAGE = "previousPage";

    static final int DEFAULT_LIST_LIMIT = 40;
    static final int MAX_LIST_LIMIT = 100;
}
