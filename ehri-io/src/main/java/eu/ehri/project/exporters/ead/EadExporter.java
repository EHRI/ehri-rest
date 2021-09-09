package eu.ehri.project.exporters.ead;

import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.models.DocumentaryUnit;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encoded Archival Description (EAD) export.
 */
public interface EadExporter extends XmlExporter<DocumentaryUnit> {

    /**
     * Export a documentary unit as an EAD document.
     *
     * @param unit         the unit
     * @param outputStream the output stream to write to.
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     */
    void export(DocumentaryUnit unit,
            OutputStream outputStream, String langCode) throws IOException;

    /**
     * Export a documentary unit as an EAD document.
     *
     * @param unit         the unit
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     * @return a DOM document
     */
    Document export(DocumentaryUnit unit, String langCode) throws IOException;
}
