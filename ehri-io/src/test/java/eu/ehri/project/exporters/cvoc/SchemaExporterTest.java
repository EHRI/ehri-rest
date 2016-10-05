package eu.ehri.project.exporters.cvoc;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import eu.ehri.project.definitions.Entities;
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