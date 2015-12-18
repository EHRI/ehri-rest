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

package eu.ehri.project.importers.cvoc;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;


public interface SkosImporter {

    /**
     * Import a file with a given log message.
     *
     * @param filePath   The file path
     * @param logMessage A log message
     * @return An import log
     * @throws IOException
     * @throws InputParseError
     * @throws ValidationError
     */
    ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError;

    /**
     * Import an input stream with a given log message.
     *
     * @param ios        The input stream
     * @param logMessage A log message
     * @return An import log
     * @throws IOException
     * @throws InputParseError
     * @throws ValidationError
     */
    ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, InputParseError, ValidationError;

    /**
     * Switch the importer mode to one that is tolerant
     * of individual item validation errors.
     *
     * @param tolerant Allow individual items to error
     * @return A new SKOS importer with the given tolerance mode
     */
    SkosImporter setTolerant(boolean tolerant);

    /**
     * Set the RDF format. Supported values are: N3, TTL, TURTLE,
     * and the default, RDF/XML.
     *
     * @param format The RDF format string.
     * @return A new SKOS importer with the given format
     */
    SkosImporter setFormat(String format);

    /**
     * Set default language for literals without a lang suffix.
     *
     * @param lang A two- or three-letter language code
     * @return A new SKOS importer with the given default language
     */
    SkosImporter setDefaultLang(String lang);
}
