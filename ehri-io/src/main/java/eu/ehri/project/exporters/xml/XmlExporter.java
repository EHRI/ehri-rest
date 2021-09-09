package eu.ehri.project.exporters.xml;

import eu.ehri.project.models.base.Entity;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;


public interface XmlExporter<T extends Entity> {
    /**
     * Export an item as an XML document.
     *
     * @param item         the item
     * @param outputStream the output stream to write to.
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     * @throws IOException if an error occurs writing to the output stream
     */
    void export(T item, OutputStream outputStream, String langCode) throws IOException;

    /**
     * Export an item as an XML document.
     *
     * @param item     the item
     * @param langCode the preferred language code when multiple
     *                 descriptions are available
     * @return a DOM document
     * @throws IOException if an error occurs creating the document
     */
    Document export(T item, String langCode) throws IOException;
}
