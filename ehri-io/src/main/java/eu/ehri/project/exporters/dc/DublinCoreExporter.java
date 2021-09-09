package eu.ehri.project.exporters.dc;

import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.models.base.Described;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;

public interface DublinCoreExporter extends XmlExporter<Described> {
    /**
     * Export an item as a DC document.
     *
     * @param item         the described item
     * @param outputStream the output stream to write to.
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     */
    void export(Described item,
            OutputStream outputStream, String langCode) throws IOException;

    /**
     * Export an item as a DC document.
     *
     * @param item     the described item
     * @param langCode the preferred language code when multiple
     *                 descriptions are available
     * @return a DOM document
     */
    Document export(Described item, String langCode) throws IOException;
}
