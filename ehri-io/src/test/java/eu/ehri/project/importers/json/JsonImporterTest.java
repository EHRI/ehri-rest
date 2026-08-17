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

package eu.ehri.project.importers.json;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.exceptions.ImportValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.JsonImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.Repository;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class JsonImporterTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";

    // A well-formed item with everything needed to import cleanly.
    private static final String VALID_ITEM_1 = "{" +
            "\"objectIdentifier\":\"item1\",\"name\":\"Test Item One\"," +
            "\"levelOfDescription\":\"collection\",\"languageCode\":\"English\"," +
            "\"sourceFileId\":\"item1\"}";
    private static final String VALID_ITEM_2 = "{" +
            "\"objectIdentifier\":\"item2\",\"name\":\"Test Item Two\"," +
            "\"levelOfDescription\":\"collection\",\"languageCode\":\"English\"," +
            "\"sourceFileId\":\"item2\"}";
    // Missing the mandatory 'objectIdentifier', which triggers a ValidationError.
    private static final String INVALID_ITEM = "{" +
            "\"name\":\"Bad Item\",\"levelOfDescription\":\"collection\"," +
            "\"languageCode\":\"English\",\"sourceFileId\":\"bad\"}";

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    private JsonImportManager manager(ImportOptions options) throws Exception {
        Repository ps = manager.getEntity(TEST_REPO, Repository.class);
        return JsonImportManager.create(graph, ps, adminUser, EadImporter.class, options)
                .withPreCallback(getPidGeneratorCallback());
    }

    @Test
    public void testImportTopLevelScalarThrowsInputParseError() throws Exception {
        // A top-level JSON scalar is neither an object nor an array: the manager
        // must surface this as an InputParseError rather than an NPE.
        try (InputStream ios = stream("42")) {
            manager(ImportOptions.basic()).importInputStream(ios, "Importing a scalar");
            fail("Importing a top-level JSON scalar should throw an InputParseError");
        } catch (InputParseError e) {
            assertThat(e.getMessage(), containsString("Expected a JSON object or array"));
        }
    }

    @Test
    public void testImportInvalidItemNotTolerantThrows() throws Exception {
        // In strict (non-tolerant) mode the first invalid item aborts the whole import.
        String json = "[" + VALID_ITEM_1 + "," + INVALID_ITEM + "," + VALID_ITEM_2 + "]";
        try (InputStream ios = stream(json)) {
            manager(ImportOptions.basic()).importInputStream(ios, "Importing with a bad item");
            fail("Importing an item with a missing identifier should throw a validation error");
        } catch (ImportValidationError e) {
            assertThat(e.getError().getMessage(), containsString("identifier"));
        }
    }

    @Test
    public void testImportSingleInvalidObjectThrows() throws Exception {
        // The single-object path must surface validation errors too.
        try (InputStream ios = stream(INVALID_ITEM)) {
            manager(ImportOptions.basic()).importInputStream(ios, "Importing a single bad item");
            fail("Importing a single invalid item should throw a validation error");
        } catch (ImportValidationError e) {
            assertThat(e.getError().getMessage(), containsString("identifier"));
        }
    }

    @Test
    public void testImportTolerantContinuesPastInvalidItem() throws Exception {
        // In tolerant mode a bad item is logged and skipped, and the remaining
        // items are still imported: with the invalid item in the middle, both
        // surrounding valid items must be created.
        String json = "[" + VALID_ITEM_1 + "," + INVALID_ITEM + "," + VALID_ITEM_2 + "]";
        try (InputStream ios = stream(json)) {
            ImportLog log = manager(ImportOptions.basic().withTolerant(true))
                    .importInputStream(ios, "Importing tolerantly");
            assertEquals(2, log.getCreated());
            // The skipped item is recorded in the log's error count.
            assertEquals(1, log.getErrored());
        }
    }

    @Test
    public void testImportItems() throws Exception {

        Repository ps = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing some Dossin records";

        int count = getNodeCount(graph);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        try (InputStream ios = ClassLoader.getSystemResourceAsStream("simple.json")) {
            ImportLog importLog = JsonImportManager.create(graph, ps, adminUser, EadImporter.class, ImportOptions.basic())
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
            assertEquals(count + 22, getNodeCount(graph));
            DocumentaryUnit unit = manager.getEntity("nl-r1-kd3", DocumentaryUnit.class);

            DocumentaryUnitDescription d1 = unit.getDescriptions().iterator().next().as(DocumentaryUnitDescription.class);
            assertEquals("eng", d1.getLanguageOfDescription());
            assertThat(d1.getProperty("languageOfMaterial"), containsInAnyOrder("nld", "eng", "fra"));
            assertEquals("Latn", d1.getProperty("scriptOfMaterial"));
            assertEquals(ps, unit.getRepository());
        }
    }
}
