package eu.ehri.project.exporters.eag;

import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.models.Repository;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Encoded Archive Guide (EAD) export.
 */
public interface EagExporter extends XmlExporter<Repository> {

    /**
     * Export a repository as an EAG document.
     *
     * @param repository   the repository
     * @param outputStream the output stream to write to.
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     * @throws IOException
     * @throws TransformerException
     */
    void export(Repository repository,
            OutputStream outputStream, String langCode) throws IOException, TransformerException;

    /**
     * Export a repository as an EAG document.
     *
     * @param repository the repository
     * @param langCode   the preferred language code when multiple
     *                   descriptions are available
     * @return a DOM document
     * @throws IOException
     */
    Document export(Repository repository, String langCode) throws IOException;
}
