package eu.ehri.project.cypher;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.rule.Neo4jRule;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class FunctionsTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withFunction(Functions.class);

    @Test
    public void testJoin() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result = session
                    .run("RETURN eu.ehri.project.cypher.join({list}, {sep}) as value",
                            Values.parameters(
                                    "list",
                                    Lists.newArrayList("foo", "bar"),
                                    "sep", ",")
                    );
            assertThat(result.single().get("value").asString(), equalTo("foo,bar"));
        }
    }
    @Test
    public void testCountryCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result = session
                    .run("RETURN eu.ehri.project.cypher.countryCodeToName({code}) as value",
                            Values.parameters("code", "us"));
            assertThat(result.single().get("value").asString(), equalTo("United States"));

            StatementResult result2 = session
                    .run("RETURN eu.ehri.project.cypher.countryCodeToName({code}) as value",
                            Values.parameters("code", null));
            assertThat(result2.single().get("value").asObject(), equalTo(null));
        }
    }

    @Test
    public void testLanguageCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result1 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName({code}) as value", Values.parameters("code", "en"));
            assertThat(result1.single().get("value").asString(), equalTo("English"));

            StatementResult result2 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName({code}) as value",
                            Values.parameters("code", "fra"));
            assertThat(result2.single().get("value").asString(), equalTo("French"));

            StatementResult result3 = session
                    .run("RETURN eu.ehri.project.cypher.languageCodeToName({code}) as value",
                            Values.parameters("code", null));
            assertThat(result3.single().get("value").asObject(), equalTo(null));
        }
    }

    @Test
    public void testToList() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {

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
                StatementResult result = session
                        .run("RETURN eu.ehri.project.cypher.coerceList({data}) as value",
                                Values.parameters("data", expected.get(i)));
                Value value = result.single().get("value");
                assertThat(value.asList().toString(), equalTo(expected.get(i + 1).toString()));
            }
        }
    }
}