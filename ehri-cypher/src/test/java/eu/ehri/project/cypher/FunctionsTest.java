package eu.ehri.project.cypher;

import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class FunctionsTest {
    private static final Config settings = Config.builder().withoutEncryption().build();

    private static Neo4j neo4j;

    @BeforeClass
    public static void setUp() {
        neo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withFunction(Functions.class)
                .build();
    }

    @Test
    public void testJoin() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {
            Record result = session
                    .run("RETURN eu.ehri.project.cypher.join($list, $sep) as value",
                            Values.parameters(
                                    "list",
                                    Lists.newArrayList("foo", "bar"),
                                    "sep", ",")
                    ).single();
            assertThat(result.get("value").asString(), equalTo("foo,bar"));
        }
    }
    @Test
    public void testCountryCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {
            Record result = session
                    .run("RETURN eu.ehri.project.cypher.countryCodeToName($code) as value",
                            Values.parameters("code", "us")).single();
            assertThat(result.get("value").asString(), equalTo("United States"));

            Record result2 = session
                    .run("RETURN eu.ehri.project.cypher.countryCodeToName($code) as value",
                            Values.parameters("code", null)).single();
            assertThat(result2.get("value").asObject(), equalTo(null));
        }
    }

    @Test
    public void testLanguageCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {
            Record result1 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName($code) as value", Values.parameters("code", "en"))
                    .single();
            assertThat(result1.get("value").asString(), equalTo("English"));

            Record result2 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName($code) as value",
                            Values.parameters("code", "fra")).single();
            assertThat(result2.get("value").asString(), equalTo("French"));

            Record result3 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName($code) as value",
                            Values.parameters("code", null)).single();
            assertThat(result3.get("value").asObject(), equalTo(null));
        }
    }

    @Test
    public void testToList() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {

            List<Object> expected = Lists.newArrayList(
                    "en", Collections.singletonList("en"),
                    Collections.singletonList("en"), Collections.singletonList("en"),
                    null, Collections.emptyList(),
                    1, Collections.singletonList(1),
                    Collections.singletonList(1), Collections.singletonList(1),
                    new Object[]{1}, Collections.singletonList(1),
                    new Object[]{"en"}, Collections.singletonList("en")
            );

            for (int i = 0; i < expected.size(); i += 2) {
                Record result = session
                        .run("RETURN eu.ehri.project.cypher.coerceList($data) as value",
                                Values.parameters("data", expected.get(i))).single();
                Value value = result.get("value");
                assertThat(value.asList().toString(), equalTo(expected.get(i + 1).toString()));
            }
        }
    }
}