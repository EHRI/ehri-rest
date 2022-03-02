/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import eu.ehri.project.oaipmh.errors.OaiPmhArgumentError;
import eu.ehri.project.oaipmh.errors.OaiPmhError;

/**
 * Supported metadata types.
 */
enum MetadataPrefix {
    oai_dc("http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/"),
    ead("http://www.loc.gov/ead/ead.xsd", "urn:isbn:1-931666-22-9"),
    ead3("https://www.loc.gov/ead/ead3_undeprecated.xsd", "http://ead3.archivists.org/schema/");

    private final String schema;
    private final String namespace;

    MetadataPrefix(String schema, String namespace) {
        this.schema = schema;
        this.namespace = namespace;
    }

    public String schema() {
        return schema;
    }

    public String namespace() {
        return namespace;
    }

    static MetadataPrefix parse(String prefix, MetadataPrefix fallback) throws OaiPmhError {
        try {
            return (prefix != null) ? MetadataPrefix.valueOf(prefix) : fallback;
        } catch (IllegalArgumentException e) {
            throw new OaiPmhArgumentError("Unsupported metadata format: " + prefix);
        }
    }
}
