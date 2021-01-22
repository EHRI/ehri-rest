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