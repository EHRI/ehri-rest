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

import com.google.common.base.Optional;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Base ImportManager.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 */
public abstract class AbstractImportManager implements ImportManager {

    private static final Logger logger = LoggerFactory
            .getLogger(AbstractImportManager.class);
    protected final FramedGraph<?> framedGraph;
    protected final PermissionScope permissionScope;
    protected final Actioner actioner;
    private boolean tolerant;

    // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    protected String currentFile;
    protected Integer currentPosition;
    protected Class<? extends AbstractImporter> importerClass;

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    the permission scope
     * @param actioner the actioner
     */
    public AbstractImportManager(
            FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner, Class<? extends AbstractImporter> importerClass) {
        this.framedGraph = graph;
        this.permissionScope = scope;
        this.actioner = actioner;
        this.importerClass = importerClass;
    }

    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     *
     * @param tolerant true means it won't validateData the xml file
     */
    public AbstractImportManager setTolerant(boolean tolerant) {
        logger.info("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;
        return this;
    }

    public boolean isTolerant() {
        return tolerant;
    }

    /**
     * Import a Description file by specifying its path.
     *
     * @param filePath   a path to a description XML file
     * @param logMessage the log message
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError {
        try (FileInputStream ios = new FileInputStream(filePath)) {
            return importFile(ios, logMessage);
        }
    }

    /**
     * Import a file, creating a new action with the given log message.
     *
     * @param ios        an input stream object
     * @param logMessage the log message
     * @return an ImportLog for the given InputStream
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     */
    @Override
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {
        try {
            // Create a new action for this import
            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            ImportLog log = new ImportLog(action);

            // Do the import...
            importFile(ios, action, log);
            // If nothing was imported, remove the action...
            if (log.hasDoneWork()) {
                action.commit();
            }

            return log;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Import multiple files in the same batch/transaction.
     *
     * @param paths      a list of file paths to description objects
     * @param logMessage a log message
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError {

        try {

            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, getLogMessage(logMessage));
            ImportLog log = new ImportLog(action);
            for (String path : paths) {
                try {
                    currentFile = path;
                    try (InputStream ios = new FileInputStream(path)) {
                        logger.info("Importing file: " + path);
                        importFile(ios, action, log);
                    }
                } catch (InvalidXmlDocument e) {
                    log.setErrored(formatErrorLocation(), e.getMessage());
                    if (!tolerant) {
                        throw e;
                    }
                }
            }

            // Only mark the transaction successful if we're
            // actually accomplished something.
            if (log.hasDoneWork()) {
                action.commit();
            }

            return log;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Import multiple files in the same batch/transaction.
     *
     * @param inputStream a compressed archive containing multiple files
     * @param logMessage  a log message
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    public ImportLog importFiles(ArchiveInputStream inputStream, String logMessage)
            throws IOException, ValidationError {

        try {

            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, getLogMessage(logMessage));
            ImportLog log = new ImportLog(action);

            ArchiveEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                try {
                    currentFile = entry.getName();
                    BoundedInputStream boundedInputStream
                            = new BoundedInputStream(inputStream, entry.getSize());
                    boundedInputStream.setPropagateClose(false);
                    logger.info("Importing file: " + currentFile);
                    importFile(boundedInputStream, action, log);
                } catch (InvalidXmlDocument e) {
                    log.setErrored(formatErrorLocation(), e.getMessage());
                    if (!tolerant) {
                        throw e;
                    }
                }
            }

            // Only mark the transaction successful if we're
            // actually accomplished something.
            if (log.hasDoneWork()) {
                action.commit();
            }

            return log;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Import an InputStream with an event context.
     *
     * @param ios          the InputStream to import
     * @param eventContext the event that this import is part of
     * @param log          an import log to write to
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidXmlDocument      if an implementing subclass encounters invalid XML
     * @throws InvalidInputFormatError
     */
    protected abstract void importFile(InputStream ios,
            ActionManager.EventContext eventContext, ImportLog log)
            throws IOException, ValidationError, InputParseError,
            InvalidXmlDocument, InvalidInputFormatError;

    private Optional<String> getLogMessage(String msg) {
        return (msg == null || msg.trim().isEmpty())
                ? Optional.<String>absent()
                : Optional.of(msg);
    }

    private String formatErrorLocation() {
        return String.format("File: %s, XML document: %d", currentFile,
                currentPosition);
    }
}
