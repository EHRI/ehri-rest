package eu.ehri.project.exporters.ead;

import eu.ehri.project.models.DocumentaryUnit;
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
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface EadExporter {

    class DocumentWriter {
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

    void export(DocumentaryUnit unit,
            OutputStream outputStream, String langCode) throws IOException, TransformerException;

    Document export(DocumentaryUnit unit, String langCode) throws IOException;
}
