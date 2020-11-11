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


public class ProceduresTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure(Procedures.class);

    @Test
    public void testCountryCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result = session
                    .run("CALL eu.ehri.project.cypher.countryCodeToName({code})", Values.parameters("code", "us"));
            assertThat(result.single().get("value").asString(), equalTo("United States"));
        }
    }

    @Test
    public void testLanguageCodeToName() {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result1 = session
                    .run("CALL eu.ehri.project.cypher.languageCodeToName({code})", Values.parameters("code", "en"));
            assertThat(result1.single().get("value").asString(), equalTo("English"));

            StatementResult result2 = session
                    .run("CALL eu.ehri.project.cypher.languageCodeToName({code})", Values.parameters("code", "fra"));
            assertThat(result2.single().get("value").asString(), equalTo("French"));
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
                        .run("CALL coerceList({data})", Values.parameters("data", expected.get(i)));
                Value value = result.single().get("value");
                assertThat(value.asList().toString(), equalTo(expected.get(i + 1).toString()));
            }
        }
    }
}