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

package eu.ehri.project.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import eu.ehri.project.test.GraphTestBase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DbUpgrader1To2Test extends GraphTestBase {

    @Test
    public void testUpgradeNode() throws Exception {

        Map<String, Object> data = new HashMap<String, Object>() {{
            put("type", "documentaryUnit");
            put("relationships", new HashMap<String, Object>() {{
                put("describes", Lists.<Object>newArrayList(
                        new HashMap<String, Object>() {{
                            put("type", "documentDescription");
                            put("relationships", new HashMap<String, Object>() {{
                                put("hasDate", Lists.<Object>newArrayList(
                                        new HashMap<String, Object>() {{
                                            put("type", "datePeriod");
                                        }}
                                ));
                            }});
                        }}
                ));
            }});
        }};

        ObjectNode jsonNode = new ObjectMapper().valueToTree(data);

        ObjectNode out = DbUpgrader1to2.upgradeNode(jsonNode);
        assertEquals("DocumentaryUnit", out.path("type").asText());
        assertEquals("DocumentaryUnitDescription", out.path("relationships")
                .path("describes").path(0).path("type").asText());
        assertEquals("DatePeriod", out.path("relationships")
                .path("describes").path(0).path("relationships")
                .path("hasDate").path(0).path("type").asText());
    }
}