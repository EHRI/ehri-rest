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
