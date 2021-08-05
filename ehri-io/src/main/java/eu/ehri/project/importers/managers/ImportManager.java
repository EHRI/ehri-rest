/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.importers.managers;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.ImportValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An Import Manager takes a single file or stream, or a list of files, and a log message
 * and imports the data from the file into the database,
 * linking the import as an action with the given log message.
 */
public interface ImportManager {

    /**
     * Import a file by specifying its path.
     *
     * @param filePath   path to the file to import
     * @param logMessage an optional message to describe the import
     * @return an ImportLog for the given file
     * @throws IOException     when reading or writing files fails
     * @throws InputParseError when parsing the file fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ImportValidationError;

    /**
     * Import an item via an input stream.
     *
     * @param stream     the input stream
     * @param logMessage an optional log message to describe the import
     * @return an ImportLog for the given stream
     * @throws IOException     when reading or writing fails
     * @throws InputParseError when parsing the stream data fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    default ImportLog importInputStream(InputStream stream, String logMessage)
            throws IOException, InputParseError, ImportValidationError {
        return importInputStream(stream, "-", logMessage);
    }

    /**
     * Import an item via an input stream.
     *
     * @param stream     the input stream
     * @param tag        an optional tag identifying the source of the stream
     * @param logMessage an optional log message to describe the import
     * @return an ImportLog for the given stream
     * @throws IOException     when reading or writing fails
     * @throws InputParseError when parsing the stream data fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    ImportLog importInputStream(InputStream stream, String tag, String logMessage)
            throws IOException, InputParseError, ImportValidationError;

    /**
     * Import multiple files via a list of file paths.
     *
     * @param json       a JSON-format stream in the form of a object
     *                   consisting of name -> URL key/values
     * @param logMessage an optional log message to describe the import
     * @return an ImportLog for the given stream
     * @throws IOException     when reading or writing fails
     * @throws InputParseError when parsing the stream data fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    ImportLog importJson(InputStream json, String logMessage)
            throws IOException, InputParseError, ImportValidationError;

    /**
     * Import multiple files via a list of file paths.
     *
     * @param filePaths  a list of file paths
     * @param logMessage an optional log message to describe the import
     * @return an ImportLog for the given stream
     * @throws IOException     when reading or writing fails
     * @throws InputParseError when parsing the stream data fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    ImportLog importFiles(List<String> filePaths, String logMessage)
            throws IOException, InputParseError, ImportValidationError;

    /**
     * Import multiple items via an archive input stream.
     *
     * @param stream     the archive input stream
     * @param logMessage an optional log message to describe the import
     * @return an ImportLog for the given stream
     * @throws IOException     when reading or writing fails
     * @throws InputParseError when parsing the stream data fails
     * @throws ImportValidationError when the content of the file is invalid
     */
    ImportLog importArchive(ArchiveInputStream stream, String logMessage)
            throws IOException, InputParseError, ImportValidationError;
}
