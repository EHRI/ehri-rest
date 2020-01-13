/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.exporters.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Stack;

/**
 * Derived from JDK6 Sun internal implementation.
 */
public class IndentingXMLStreamWriter implements XMLStreamWriter, AutoCloseable {

    private enum State {
        SEEN_NOTHING, SEEN_ELEMENT, SEEN_DATA
    }

    private State state = State.SEEN_NOTHING;
    private Stack<State> stateStack = new Stack<>();

    private final String indentStep;
    private int depth = 0;

    private final XMLStreamWriter writer;

    public IndentingXMLStreamWriter(XMLStreamWriter writer, String indentStep) {
        this.writer = writer;
        this.indentStep = indentStep;
    }

    public IndentingXMLStreamWriter(XMLStreamWriter writer) {
        this(writer, "  ");
    }

    private void onStartElement() throws XMLStreamException {
        stateStack.push(State.SEEN_ELEMENT);
        state = State.SEEN_NOTHING;
        if (depth > 0) {
            writer.writeCharacters("\n");
        }
        doIndent();
        depth++;
    }

    private void onEndElement() throws XMLStreamException {
        depth--;
        if (state == State.SEEN_ELEMENT) {
            writer.writeCharacters("\n");
            doIndent();
        }
        state = stateStack.pop();
    }

    private void onEmptyElement() throws XMLStreamException {
        state = State.SEEN_ELEMENT;
        if (depth > 0) {
            writer.writeCharacters("\n");
        }
        doIndent();
    }

    private void doIndent() throws XMLStreamException {
        if (depth > 0) {
            for (int i = 0; i < depth; i++)
                writer.writeCharacters(indentStep);
        }
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        writer.writeStartDocument();
        writer.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writer.writeStartDocument(version);
        writer.writeCharacters("\n");
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writer.writeStartDocument(encoding, version);
        writer.writeCharacters("\n");
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        onStartElement();
        writer.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        onStartElement();
        writer.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onStartElement();
        writer.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        onEmptyElement();
        writer.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onEmptyElement();
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        onEmptyElement();
        writer.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        onEndElement();
        writer.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        writer.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        writer.flush();
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writer.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writer.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        writer.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        writer.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        writer.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        writer.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        writer.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        state = State.SEEN_DATA;
        writer.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        state = State.SEEN_DATA;
        writer.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return null;
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {

    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {

    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {

    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        state = State.SEEN_DATA;
        writer.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {

    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {

    }
}
