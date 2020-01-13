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

import com.google.common.base.Preconditions;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.ModeViolation;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Mutation;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Base ImportManager.
 */
public abstract class AbstractImportManager implements ImportManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractImportManager.class);
    protected final FramedGraph<?> framedGraph;
    protected final PermissionScope permissionScope;
    protected final Actioner actioner;
    protected final boolean tolerant;
    protected final boolean allowUpdates;
    protected final String defaultLang;

    // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    private String currentFile;
    protected Integer currentPosition;
    protected final Class<? extends ItemImporter<?,?>> importerClass;

    /**
     * Constructor.
     *
     * @param graph         the framed graph
     * @param scope         the permission scope
     * @param actioner      the actioner
     * @param tolerant      allow individual items to fail validation without
     *                      failing an entire batch
     * @param allowUpdates  allow this import manager to update data items as well
     *                      as create them
     * @param importerClass the class of the item importer object
     */
    public AbstractImportManager(
            FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner,
            boolean tolerant,
            boolean allowUpdates,
            String defaultLang,
            Class<? extends ItemImporter<?,?>> importerClass) {
        Preconditions.checkNotNull(scope, "Scope cannot be null");
        this.framedGraph = graph;
        this.permissionScope = scope;
        this.actioner = actioner;
        this.tolerant = tolerant;
        this.allowUpdates = allowUpdates;
        this.defaultLang = defaultLang;
        this.importerClass = importerClass;
    }

    /**
     * Determine if the importer is in tolerant mode.
     *
     * @return a boolean value
     */
    public boolean isTolerant() {
        return tolerant;
    }

    @Override
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError {
        try (InputStream ios = Files.newInputStream(Paths.get(filePath))) {
            return importInputStream(ios, filePath, logMessage);
        }
    }

    @Override
    public ImportLog importInputStream(InputStream stream, String tag, String logMessage)
            throws IOException, InputParseError, ValidationError {
        // Create a new action for this import
        Optional<String> msg = getLogMessage(logMessage);
        ActionManager.EventContext action = new ActionManager(
                framedGraph, permissionScope).newEventContext(actioner,
                EventTypes.ingest, msg);
        // Create a manifest to store the results of the import.
        ImportLog log = new ImportLog(msg.orElse(null));

        // Do the import...
        importInputStream(stream, tag, action, log);
        // If nothing was imported, remove the action...
        if (log.hasDoneWork()) {
            action.commit();
        }

        return log;
    }

    @Override
    public ImportLog importFiles(List<String> filePaths, String logMessage)
            throws IOException, ValidationError, InputParseError {
        try {

            Optional<String> msg = getLogMessage(logMessage);
            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, msg);
            ImportLog log = new ImportLog(msg.orElse(null));
            for (String path : filePaths) {
                try {
                    currentFile = path;
                    try (InputStream stream = Files.newInputStream(Paths.get(path))) {
                        logger.info("Importing file: {}", path);
                        importInputStream(stream, currentFile, action, log);
                    }
                } catch (ValidationError e) {
                    log.addError(formatErrorLocation(), e.getMessage());
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

    @Override
    public ImportLog importArchive(ArchiveInputStream stream, String logMessage)
            throws IOException, InputParseError, ValidationError {
        Optional<String> msg = getLogMessage(logMessage);
        ActionManager.EventContext action = new ActionManager(
                framedGraph, permissionScope).newEventContext(actioner,
                EventTypes.ingest, msg);
        ImportLog log = new ImportLog(msg.orElse(null));

        ArchiveEntry entry;
        while ((entry = stream.getNextEntry()) != null) {
            try {
                if (!entry.isDirectory()) {
                    currentFile = entry.getName();
                    BoundedInputStream boundedInputStream
                            = new BoundedInputStream(stream, entry.getSize());
                    boundedInputStream.setPropagateClose(false);
                    logger.info("Importing file: {}", currentFile);
                    importInputStream(boundedInputStream, currentFile, action, log);
                }
            } catch (InputParseError | ValidationError e) {
                log.addError(formatErrorLocation(), e.getMessage());
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
    }

    /**
     * Import an InputStream with an event context.
     *
     * @param stream  the InputStream to import
     * @param tag        an optional tag identifying the source of the stream
     * @param context the event that this import is part of
     * @param log     an import log to write to
     */
    protected abstract void importInputStream(InputStream stream,
            String tag, ActionManager.EventContext context, ImportLog log)
            throws IOException, ValidationError, InputParseError;

    /**
     * A default handler for import callbacks which adds the item to the
     * log and event context.
     *
     * @param log      an import log
     * @param context  an event context
     * @param mutation the item mutation
     */
    void defaultImportCallback(ImportLog log, ActionManager.EventContext context, Mutation<? extends Accessible> mutation) {
        switch (mutation.getState()) {
            case CREATED:
                logger.info("Item created: {}", mutation.getNode().getId());
                context.addSubjects(mutation.getNode());
                log.addCreated();
                break;
            case UPDATED:
                if (!allowUpdates) {
                    throw new ModeViolation(String.format(
                            "Item '%s' was updated but import manager does not allow updates",
                            mutation.getNode().getId()));
                }
                logger.info("Item updated: {}", mutation.getNode().getId());
                context.addSubjects(mutation.getNode());
                log.addUpdated();
                break;
            default:
                log.addUnchanged();
        }
    }

    /**
     * A default handler for error callbacks which adds the error to
     * the log and throws it if the importer is not in tolerant mode.
     *
     * @param log an import log
     * @param ex  the propagated exception
     */
    void defaultErrorCallback(ImportLog log, Exception ex) {
        // Otherwise, check if we had a validation error that was
        // thrown for an individual item and only re-throw if
        // tolerant is off.
        if (ex instanceof ValidationError) {
            ValidationError e = (ValidationError) ex;
            log.addError(e.getBundle().getId(), e.getErrorSet().toString());
            if (!isTolerant()) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(ex);
        }
    }

    // Helpers

    private Optional<String> getLogMessage(String msg) {
        return (msg == null || msg.trim().isEmpty())
                ? Optional.empty()
                : Optional.of(msg);
    }

    private String formatErrorLocation() {
        return String.format("File: %s, XML document: %d", currentFile,
                currentPosition);
    }
}
