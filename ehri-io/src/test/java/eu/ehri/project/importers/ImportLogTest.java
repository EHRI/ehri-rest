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

package eu.ehri.project.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;


public class ImportLogTest {
    private final ObjectMapper mapper = new JsonMapper();

    @Test
    public void testDeserialize() throws Exception {
        ImportLog log = mapper.readValue("{" +
                "\"message\":\"test\", " +
                "\"errors\": {}, " +
                "\"created_keys\": {}, " +
                "\"created\": 0," +
                "\"updated_keys\": {}, " +
                "\"updated\": 0," +
                "\"unchanged_keys\": {}," +
                "\"unchanged\": 0" +
                "}", ImportLog.class);
        assertEquals(new ImportLog("test"), log);
    }

    @Test
    public void testSerialize() throws Exception {
        ImportLog log = new ImportLog("test");
        String json = mapper.writeValueAsString(log);
        assertThat(json, containsString("\"created\":0"));
    }
}