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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphQLImplTest extends AbstractFixtureTest {
    @Test
    public void testQuery() throws Exception {
        JsonNode data = execute("testquery.graphql", Collections.emptyMap());
        // System.out.println(data.toPrettyString());
        String arkPrefix = ConfigFactory.load().getString("io.pids.prefix");

        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertEquals("c1-12345678", data.path("data").path("c1").path("pid").textValue());
        assertEquals(0, data.path("data").path("c1").path("ancestors").size());
        assertEquals(1, data.path("data").path("c1")
                .path("children").path("items").size());
        assertEquals(2, data.path("data").path("c1")
                .path("allChildren").path("items").size());
        assertFalse(data.path("data").path("c1").path("itemCount").isMissingNode());
        assertEquals(1, data.path("data").path("c1").path("itemCount").intValue());
        assertEquals("c1-alt", data.path("data").path("c1").path("otherIdentifiers").path(0).textValue());
        assertTrue(data.path("data").path("c1").path("firstEnglishDescription")
                .path("dates").findValuesAsText("precision").contains("year"));
        assertEquals(0, data.path("data").path("c4").path("itemCount").intValue());
        assertEquals("c2", data.path("data").path("c1")
                .path("children").path("items").path(0)
                .path("id").textValue());
        assertEquals("c3", data.path("data").path("c1")
                .path("children").path("items").path(0)
                .path("children").path("items").path(0)
                .path("id").textValue());
        assertEquals("a2", data.path("data").path("c3").path("related")
                .path(0).path("item").path("id").textValue());
        assertEquals("ur3", data.path("data").path("c3").path("related")
                .path(0).path("context").path("body").path(0).path("id").textValue());
        assertEquals("Person Access 2", data.path("data").path("c3").path("related")
                .path(0).path("context").path("body").path(0).path("name").textValue());
        assertEquals("r1-1234", data.path("data").path("c1").path("repository")
                .path("pid").textValue());
        assertEquals(arkPrefix + "r1-1234", data.path("data").path("c1").path("repository")
                        .path("ark").textValue());
        assertEquals("An Address", data.path("data").path("c1").path("repository")
                .path("english").path("addresses").path(0)
                .path("addressName").textValue());
        assertEquals("Amsterdam", data.path("data").path("c1").path("repository")
                .path("english").path("addresses").path(0)
                .path("municipality").textValue());
        assertEquals("test@example.com", data.path("data").path("c1")
                .path("repository").path("english").path("addresses").path(0)
                .path("email").path(0).textValue());
        assertEquals(2, data.path("data").path("c3").path("ancestors").size());
        assertEquals("c2", data.path("data").path("c3").path("ancestors")
                .path(0).path("id").textValue());
        assertEquals("c1", data.path("data").path("c3").path("ancestors")
                .path(1).path("id").textValue());
        assertEquals("ann7", data.path("data").path("c4")
                .path("annotations").path(0).path("id").textValue());
        assertEquals("scopeAndContent", data.path("data").path("c3")
                .path("annotations").path(0).path("field").textValue());
        assertEquals("Mike", data.path("data").path("c3")
                .path("annotations").path(0).path("by").textValue());
        assertFalse(data.path("data").path("topLevelOnly")
                .path("items").path(0).path("id").isMissingNode());
        assertEquals(3, data.path("data").path("topLevelOnly")
                .path("items").size());
        assertEquals(5, data.path("data").path("allLevels")
                .path("items").size());
        assertFalse(data.path("data").path("topLevelDocumentaryUnits")
                .path("items").path(0).path("id").isMissingNode());
        assertEquals("c4", data.path("data").path("r4").path("links")
                .path(0).path("targets").path(0).path("id").textValue());
        assertEquals("cvocc1", data.path("data").path("cvocc2").path("related")
                .path(0).path("id").textValue());
        assertEquals("Subject Access 2", data.path("data").path("cvocc2").path("connected")
                .path(0).path("context").path("body").path(0).path("name").textValue());
        assertEquals("Test", data.path("data").path("gb").path("summary").textValue());
        assertEquals("Test", data.path("data").path("gb").path("situation").textValue());
        assertEquals("Test", data.path("data").path("gb").path("history").textValue());
        assertEquals("Test", data.path("data").path("gb").path("extensive").textValue());
        assertFalse(data.path("data").path("wrongType").isMissingNode());
        assertTrue(data.path("data").path("wrongType").isNull());
        assertTrue(data.path("data").path("link3").path("source").isNull());
        assertEquals(2, data.path("data").path("link3").path("targets").size());
        assertEquals("associative", data.path("data").path("link3").path("linkType").textValue());
        assertEquals("c4", data.path("data").path("link4").path("source").path("id").textValue());
        assertEquals("copy", data.path("data").path("link4").path("linkType").textValue());
        assertEquals("r1", data.path("data").path("itemByPid").path("id").textValue());
        assertEquals("http://www.example.com/authorities/a1", data.path("data").path("firstAgentDesc")
                .path("seeAlso").path(0).textValue());
    }

    @Test
    public void testQueryConnection() throws Exception {
        JsonNode data = execute("testquery-connection.graphql", Collections.emptyMap());
        assertTrue(data.path("data").path("empty").path("pageInfo").path("hasPreviousPage").asBoolean());
        assertFalse(data.path("data").path("empty").path("pageInfo").path("hasNextPage").asBoolean());
    }

    @Test
    public void testQueryVariables() throws Exception {
        Map<String, Object> vars = Maps.newHashMap();
        vars.put("n", 4);
        JsonNode data = execute("testquery-variables.graphql", vars);
        assertEquals(4, data.path("data").path("test").path("items").size());
        assertFalse(data.path("data").path("test").path("pageInfo").path("nextPage").isNull());

        vars.put("from", data.path("data").path("test").path("pageInfo").path("nextPage").textValue());
        JsonNode nextData = execute("testquery-variables.graphql", vars);
        assertEquals(1, nextData.path("data").path("test").path("items").size());
    }

    @Test
    public void testQueryAccessPointTypes() throws Exception {
        JsonNode data = execute("testquery-accesspoints.graphql", Collections.emptyMap()).path("data");
        JsonNode c1 = data.path("c1");
        // link2: c1 -> a1, via c1's subject access point
        assertEquals(1, c1.path("connectedSubjects").size());
        assertEquals("a1", c1.path("connectedSubjects").path(0).path("item").path("id").textValue());
        assertEquals(0, c1.path("connectedPeople").size());
        assertEquals(1, c1.path("connectedAssociativeSubjects").size());
        assertEquals(1, c1.path("connectedSubjectsOrPeople").size());
        assertEquals(0, c1.path("incomingSubjects").size());
        // excludes link1, which has no access point
        assertEquals(1, c1.path("outgoingAny").size());
        assertEquals("a1", c1.path("outgoingAny").path(0).path("item").path("id").textValue());
        assertTrue(c1.path("all").size() > 1);

        // From a1, link2 is incoming
        JsonNode a1 = data.path("a1");
        assertEquals(1, a1.path("subjects").size());
        assertEquals(0, a1.path("outgoingSubjects").size());
        assertEquals(1, a1.path("incomingSubjects").size());
        assertEquals("c1", a1.path("incomingSubjects").path(0).path("item").path("id").textValue());
        assertEquals(1, a1.path("allSubjects").size());

        JsonNode desc = c1.path("description");
        assertEquals(2, desc.path("accessPoints").size());
        assertEquals(1, desc.path("subjectAccessPoints").size());
        assertEquals("Subject Access 1", desc.path("subjectAccessPoints").path(0).path("name").textValue());
        assertEquals(0, desc.path("placeAccessPoints").size());
        assertEquals(2, desc.path("subjectOrPersonAccessPoints").size());
        assertEquals(1, desc.path("subjectOrPlaceAccessPoints").size());
    }

    @Test
    public void testQueryByIds() throws Exception {
        JsonNode data = execute("testquery-ids.graphql", Collections.emptyMap()).path("data");
        assertEquals(Lists.newArrayList("c2", "c1", null, null, "c1"), ids(data.path("docs").path("items")));
        assertEquals(Lists.newArrayList("c2"), ids(data.path("firstDoc").path("items")));
        assertTrue(data.path("firstDoc").path("pageInfo").path("hasNextPage").booleanValue());
        assertEquals(Lists.newArrayList("r1", null), ids(data.path("repositories").path("items")));
        // A cursor of -1 is treated as the start
        JsonNode negativeFrom = data.path("negativeFrom");
        assertEquals(Lists.newArrayList("c2"), ids(negativeFrom.path("items")));
        assertEquals("MA==", negativeFrom.path("edges").path(0).path("cursor").textValue());
        assertFalse(negativeFrom.path("pageInfo").path("hasPreviousPage").booleanValue());
        assertEquals("MQ==", negativeFrom.path("pageInfo").path("nextPage").textValue());
    }

    @Test
    public void testQueryByIdsWithPermissions() throws Exception {
        Map<String, Object> vars = Collections.singletonMap("ids", Lists.newArrayList("c1", "c4"));
        JsonNode data = execute("testquery-ids-variable.graphql", vars, basicUser).path("data");
        assertEquals(Lists.newArrayList(null, "c4"), ids(data.path("documentaryUnits").path("items")));
    }

    static List<String> ids(JsonNode items) {
        List<String> ids = Lists.newArrayList();
        items.forEach(item -> ids.add(item.path("id").textValue()));
        return ids;
    }

    private JsonNode execute(String queryResource, Map<String, Object> vars) throws Exception {
        return execute(queryResource, vars, adminUser);
    }

    private JsonNode execute(String queryResource, Map<String, Object> vars, Accessor accessor) throws Exception {
        GraphQLSchema schema = new GraphQLImpl(api(accessor)).getSchema();
        ExecutionResult result = GraphQL.newGraphQL(schema).build()
                .execute(ExecutionInput.newExecutionInput()
                        .query(readResourceFileAsString(queryResource))
                        .variables(vars)
                        .build());
        assertTrue(result.getErrors().toString(), result.getErrors().isEmpty());
        return new ObjectMapper().valueToTree(result.toSpecification());
    }
}
