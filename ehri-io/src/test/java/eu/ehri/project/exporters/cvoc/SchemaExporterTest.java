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

package eu.ehri.project.exporters.cvoc;

import eu.ehri.project.definitions.Entities;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertTrue;

public class SchemaExporterTest {

    @Test
    public void testDumpSchema() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SchemaExporter("TTL").dumpSchema(outputStream, null);
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(SchemaExporter.NAMESPACES);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            model.read(inputStream, null, "TTL");
            //System.out.println(outputStream.toString("UTF-8"));
            assertTrue(model.containsResource(model.createResource(SchemaExporter.DEFAULT_BASE_URI + Entities
                    .USER_PROFILE)));
        }
    }
}