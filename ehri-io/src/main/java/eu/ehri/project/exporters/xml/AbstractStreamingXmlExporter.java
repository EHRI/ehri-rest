package eu.ehri.project.exporters.xml;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import eu.ehri.project.models.base.Entity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class AbstractStreamingXmlExporter<E extends Entity>
        extends StreamingXmlDsl
        implements StreamingXmlExporter<E>, XmlExporter<E> {

    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

    @Override
    public Document export(E item, String langCode) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            export(item, baos, langCode);
            return new DocumentReader().read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (ParserConfigurationException | TransformerException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void export(E unit, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        try (final IndentingXMLStreamWriter sw = new IndentingXMLStreamWriter(
                xmlOutputFactory.createXMLStreamWriter(new BufferedOutputStream(outputStream)))) {
            sw.writeStartDocument();
            export(sw, unit, langCode);
            sw.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Object> coerceList(Object value) {
        return value == null ? ImmutableList.of()
                : (value instanceof List ? (List<Object>) value : ImmutableList.of(value));
    }

    protected String resourceAsString(String resourceName) {
        try {
            return Resources.toString(Resources.getResource(resourceName),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
