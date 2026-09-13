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

package eu.ehri.project.importers.csv;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.Repository;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;


public class CsvImporterTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItems() throws Exception {

        Repository ps = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing some Dossin records";

        int count = getNodeCount(graph);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        try (InputStream ios = ClassLoader.getSystemResourceAsStream("simple.csv")) {
            ImportLog importLog = CsvImportManager.create(graph, ps, adminUser, EadImporter.class,
                            ImportOptions.basic()
                                    .withFieldSeparator(',')
                                    .withArraySeparator("||"))
                    .withPreCallback(getPidGeneratorCallback())
                    .importInputStream(ios, logMessage);
            System.out.println(importLog);
            // After...
            List<VertexProxy> graphState2 = getGraphState(graph);
            GraphDiff diff = diffGraph(graphState1, graphState2);
            diff.printDebug(System.out);
            /*
             * null: 5
             * relationship: 4
             * DocumentaryUnit: 4
             * documentDescription: 4
             * systemEvent: 1
             * datePeriod: 4
             */
            assertEquals(count + 27, getNodeCount(graph));
            DocumentaryUnit unit = manager.getEntity("nl-r1-kd3", DocumentaryUnit.class);

            DocumentaryUnitDescription d1 = unit.getDescriptions().iterator().next().as(DocumentaryUnitDescription.class);
            assertEquals("eng", d1.getLanguageOfDescription());
            assertThat(d1.getProperty("languageOfMaterial"), containsInAnyOrder("nld", "eng", "fra"));
            assertThat(d1.getProperty("scriptOfMaterial"), containsInAnyOrder("Latn"));
            assertEquals(ps, unit.getRepository());

            // A single-valued flat access point column, e.g. "creatorAccessPoint,Kazerne Dossin"
            DocumentaryUnit kd1 = manager.getEntity("nl-r1-kd1", DocumentaryUnit.class);
            DocumentaryUnitDescription kd1desc = kd1.getDescriptions().iterator().next().as(DocumentaryUnitDescription.class);
            List<String> kd1AccessPointNames = accessPointNames(kd1desc.getAccessPoints());
            assertThat(kd1AccessPointNames, containsInAnyOrder("Kazerne Dossin", "Holocaust", "Human Rights"));
            for (AccessPoint ap : kd1desc.getAccessPoints()) {
                if (ap.getName().equals("Kazerne Dossin")) {
                    assertEquals(AccessPointType.creator, ap.getRelationshipType());
                } else {
                    assertEquals(AccessPointType.subject, ap.getRelationshipType());
                }
            }

            // A multi-valued flat access point column, split on "||", e.g.
            // "subjectAccessPoint,Anti-semitism||Genocide"
            DocumentaryUnit kd4 = manager.getEntity("nl-r1-kd4", DocumentaryUnit.class);
            DocumentaryUnitDescription kd4desc = kd4.getDescriptions().iterator().next().as(DocumentaryUnitDescription.class);
            List<String> kd4AccessPointNames = accessPointNames(kd4desc.getAccessPoints());
            assertThat(kd4AccessPointNames, containsInAnyOrder(
                    "Kazerne Dossin", "Anti-semitism", "Genocide"));
        }
    }

    private static List<String> accessPointNames(Iterable<AccessPoint> accessPoints) {
        List<String> names = Lists.newArrayList();
        for (AccessPoint ap : accessPoints) {
            names.add(ap.getName());
        }
        return names;
    }
}
