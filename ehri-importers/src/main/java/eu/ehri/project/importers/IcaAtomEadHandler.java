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

package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler of EADs exported from ICA-AtoM.
 * Only titles have to be handled in a special way.
 * makes use of icaatom.properties with format: part/of/path/=attribute
 */
public class IcaAtomEadHandler extends EadHandler {

    private static final Logger logger = LoggerFactory.getLogger(IcaAtomEadHandler.class);
    private final List<DocumentaryUnit>[] children = new ArrayList[12];

    /**
     * Set a custom resolver so EAD DTDs are never looked up online.
     *
     * @param publicId the public component of the EAD DTD
     * @param systemId the system component of the EAD DTD
     * @return returns essentially an empty dtd file
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
            throws org.xml.sax.SAXException, java.io.IOException {
        // This is the equivalent of returning a null dtd.
        return new org.xml.sax.InputSource(new java.io.StringReader(""));
    }

    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super(importer, properties);
        children[depth] = Lists.newArrayList();
    }

    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties("icaatom.properties"));
    }

    @Override
    protected void extractTitle(Map<String, Object> currentGraph) {
        if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
            //finding some name for this unit:
            if (currentGraph.containsKey("title")) {
                currentGraph.put(Ontology.NAME_KEY, currentGraph.get("title"));
            } else {
                logger.warn("DocumentaryUnit node without name field: ");
                currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title");
            }
        }
    }
}
