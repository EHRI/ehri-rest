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

package eu.ehri.project.cypher;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

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