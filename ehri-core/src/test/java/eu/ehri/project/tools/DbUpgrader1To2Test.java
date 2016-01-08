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