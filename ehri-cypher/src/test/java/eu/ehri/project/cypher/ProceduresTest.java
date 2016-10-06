package eu.ehri.project.cypher;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ProceduresTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withProcedure(Procedures.class);

    @Test
    public void testCountryCodeToName() throws Exception {
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withEncryptionLevel(Config
                .EncryptionLevel.NONE).toConfig()); Session session = driver.session()) {
            StatementResult result = session
                    .run("CALL eu.ehri.project.cypher.countryCodeToName({code})", Values.parameters("code", "us"));
            assertThat(result.single().get("value").asString(), equalTo("United States"));
        }
    }

    @Test
    public void testLanguageCodeToName() throws Exception {
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
}