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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

import static eu.ehri.project.graphql.GraphQLConstants.DEFAULT_LIST_LIMIT;
import static eu.ehri.project.graphql.GraphQLConstants.MAX_LIST_LIMIT;

/**
 * Helpers for encoding/decoding opaque list cursors and computing
 * page limits and offsets from GraphQL connection arguments.
 */
final class Cursors {

    private Cursors() {
    }

    static String toBase64(String s) {
        return Base64.encodeBase64String(s.getBytes());
    }

    static String fromBase64(String s) {
        return new String(Base64.decodeBase64(s));
    }

    static Map<String, Object> mapOf(Object... items) {
        Preconditions.checkArgument(items.length % 2 == 0, "Items must be pairs of key/value");
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < items.length; i += 2) {
            map.put(((String) items[i]), items[i + 1]);
        }
        return map;
    }

    static int decodeCursor(String cursor, int defaultVal) {
        try {
            return cursor != null
                    ? Math.max(-1, Integer.parseInt(fromBase64(cursor)))
                    : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    static int getLimit(Integer limitArg, boolean stream) {
        if (limitArg == null) {
            return stream ? -1 : DEFAULT_LIST_LIMIT;
        } else if (limitArg < 0) {
            return stream ? limitArg : MAX_LIST_LIMIT;
        } else {
            return stream ? limitArg : Math.min(MAX_LIST_LIMIT, limitArg);
        }
    }

    static int getOffset(String afterCursor, String fromCursor) {
        if (afterCursor != null) {
            return decodeCursor(afterCursor, -1) + 1;
        } else if (fromCursor != null) {
            return decodeCursor(fromCursor, 0);
        } else {
            return 0;
        }
    }
}
