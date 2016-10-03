package eu.ehri.project.exporters.xml;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper for writing XML documents to a stream, with
 * configurable indent.
 */
public class DocumentWriter {
    private final Document document;
    private final int indent;

    public DocumentWriter(Document document, int indent) {
        this.document = document;
        this.indent = indent;
    }

    public DocumentWriter(Document document) {
        this(document, 4);
    }

    public void write(OutputStream outputStream) throws IOException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                String.valueOf(indent));

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(outputStream);
        transformer.transform(source, result);
    }
}
