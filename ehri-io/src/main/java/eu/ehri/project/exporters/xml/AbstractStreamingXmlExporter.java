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

package eu.ehri.project.exporters.xml;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.models.base.Entity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
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

    protected static final Config config = ConfigFactory.load();
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

    @Override
    public Document export(E item, String langCode) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            export(item, baos, langCode);
            return new DocumentReader().read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void export(E unit, OutputStream outputStream, String langCode) throws IOException {
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
