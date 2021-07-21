package eu.ehri.project.cypher;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.*;
import org.neo4j.harness.junit.rule.Neo4jRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class ProceduresTest {
    private static final Config settings = Config.builder().withoutEncryption().build();

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure(Procedures.class);

    @Test
    public void testCountryCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {
            Record result = session
                    .run("CALL eu.ehri.project.cypher.countryCodeToName($code)", Values.parameters("code", "us")).single();
            assertThat(result.get("value").asString(), equalTo("United States"));
        }
    }

    @Test
    public void testLanguageCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), settings); Session session = driver.session()) {
            Record result1 = session
                    .run("CALL eu.ehri.project.cypher.languageCodeToName($code)", Values.parameters("code", "en")).single();
            assertThat(result1.get("value").asString(), equalTo("English"));

            Record result2 = session
                    .run("CALL eu.ehri.project.cypher.languageCodeToName($code)", Values.parameters("code", "fra")).single();
            assertThat(result2.get("value").asString(), equalTo("French"));
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
                        .run("CALL coerceList($data)", Values.parameters("data", expected.get(i))).single();
                Value value = result.get("value");
                assertThat(value.asList().toString(), equalTo(expected.get(i + 1).toString()));
            }
        }
    }
}