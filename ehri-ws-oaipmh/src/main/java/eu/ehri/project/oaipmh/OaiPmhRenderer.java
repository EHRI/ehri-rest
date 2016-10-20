/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.oaipmh;

import eu.ehri.project.api.Api;
import eu.ehri.project.exporters.dc.DublinCore11Exporter;
import eu.ehri.project.exporters.ead.Ead2002Exporter;
import eu.ehri.project.models.DocumentaryUnit;

import javax.xml.stream.XMLStreamWriter;

/**
 * A function that renders XML to a stream writer depending on the
 * given metadata prefix and item.
 */
@FunctionalInterface
public interface OaiPmhRenderer {
    void render(XMLStreamWriter sw, MetadataPrefix mdp, DocumentaryUnit item);

    /**
     * The default renderer, which handles OAI DC and EAD.
     *
     * @param api      the Api instance
     * @param langCode the language code to prefer
     * @return a renderer object
     */
    public static OaiPmhRenderer defaultRenderer(Api api, String langCode) {
        return (w, mp, item) -> {
            if (MetadataPrefix.ead.equals(mp)) {
                new Ead2002Exporter(api).export(w, item, langCode);
            } else {
                new DublinCore11Exporter(api).export(w, item, langCode);
            }
        };
    }
}
