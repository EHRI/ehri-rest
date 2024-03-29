/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
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

package eu.ehri.project.importers.managers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.exceptions.ImportValidationError;
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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Base ImportManager.
 */
public abstract class AbstractImportManager implements ImportManager {

    private static final int MAX_RETRIES = 3;
    private static final Logger logger = LoggerFactory.getLogger(AbstractImportManager.class);
    private final JsonFactory factory = new JsonFactory();

    protected final FramedGraph<?> framedGraph;
    protected final PermissionScope permissionScope;
    protected final Actioner actioner;
    protected final ImportOptions options;

    // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    protected String currentFile;
    protected Integer currentPosition;
    protected final Class<? extends ItemImporter<?, ?>> importerClass;
    private int consecutiveIoErrors = 0;

    /**
     * Constructor.
     *
     * @param graph         the framed graph
     * @param scope         the permission scope
     * @param actioner      the actioner
     * @param importerClass the class of the item importer object
     * @param options       an import options instance
     */
    public AbstractImportManager(
            FramedGraph<?> graph,
            PermissionScope scope,
            Actioner actioner,
            Class<? extends ItemImporter<?, ?>> importerClass,
            ImportOptions options) {
        Preconditions.checkNotNull(scope, "Scope cannot be null");
        this.framedGraph = graph;
        this.permissionScope = scope;
        this.actioner = actioner;
        this.importerClass = importerClass;
        this.options = options;
    }

    /**
     * Determine if the importer is in tolerant mode.
     *
     * @return a boolean value
     */
    public boolean isTolerant() {
        return options.tolerant;
    }

    @Override
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ImportValidationError {
        try (InputStream ios = Files.newInputStream(Paths.get(filePath))) {
            return importInputStream(ios, filePath, logMessage);
        }
    }

    @Override
    public ImportLog importInputStream(InputStream stream, String tag, String logMessage)
            throws IOException, InputParseError, ImportValidationError {
        // Create a new action for this import
        Optional<String> msg = getLogMessage(logMessage);
        ActionManager.EventContext action = new ActionManager(
                framedGraph, permissionScope).newEventContext(actioner,
                EventTypes.ingest, msg);
        // Create a manifest to store the results of the import.
        ImportLog log = new ImportLog(msg.orElse(null));

        // Do the import...
        try {
            importInputStream(stream, tag, action, log);
        } catch (ValidationError e) {
            throw new ImportValidationError(e.getMessage(), e);
        }

        // Commit the action if necessary
        return log.committing(action);
    }

    @Override
    public ImportLog importJson(InputStream json, String logMessage) throws ImportValidationError, InputParseError {
        Preconditions.checkNotNull(json);
        try (final JsonParser parser = factory.createParser(new InputStreamReader(json, Charsets.UTF_8))) {

            JsonToken jsonToken = parser.nextValue();
            if (!parser.isExpectedStartObjectToken()) {
                throw new InputParseError("Stream should be an object of name/URL pairs, was: " + jsonToken);
            }

            Optional<String> msg = getLogMessage(logMessage);
            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, msg);
            ImportLog log = new ImportLog(msg.orElse(null));

            for (int i = 1; (currentFile = parser.nextFieldName()) != null; i++) {
                URL url = new URL(parser.nextTextValue());

                try (InputStream stream = readUrl(url, 0)) {
                    logger.info("Importing URL {} with identifier: {}", i, currentFile);
                    importInputStream(stream, currentFile, action, log);
                } catch (ValidationError e) {
                    log.addError(formatErrorLocation(), e.getMessage());
                    if (!options.tolerant) {
                        // when importing from an input stream there is no point giving
                        // a formatted error location since the XML file is always null
                        throw new ImportValidationError(e.getMessage(), e);
                    }
                } catch (IOException | InputParseError e) {
                    e.printStackTrace();
                    log.addError(formatErrorLocation(), e.getMessage());
                    // Error regardless of tolerant setting if more than 5 consecutive items
                    // fail to download... this prevents an endless blocking stall...
                    if (!options.tolerant || consecutiveIoErrors > (MAX_RETRIES * 5)) {
                        throw e;
                    }
                }
            }

            // Only mark the transaction successful if we're
            // actually accomplished something.
            return log.committing(action);
        } catch (JsonParseException e) {
            throw new InputParseError("Error reading JSON", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InputParseError(e.getMessage());
        }
    }

    @Override
    public ImportLog importFiles(List<String> filePaths, String logMessage)
            throws ImportValidationError, InputParseError {
        try {

            Optional<String> msg = getLogMessage(logMessage);
            ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).newEventContext(actioner,
                    EventTypes.ingest, msg);
            ImportLog log = new ImportLog(msg.orElse(null));
            for (String path : filePaths) {
                currentFile = path;
                try (InputStream stream = Files.newInputStream(Paths.get(path))) {
                    logger.info("Importing file: {}", path);
                    importInputStream(stream, currentFile, action, log);
                } catch (ValidationError e) {
                    log.addError(formatErrorLocation(), e.getMessage());
                    if (!options.tolerant) {
                        throw new ImportValidationError(formatErrorLocation(), e);
                    }
                }
            }

            // Only mark the transaction successful if we're
            // actually accomplished something.
            return log.committing(action);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportLog importArchive(ArchiveInputStream stream, String logMessage)
            throws IOException, InputParseError, ImportValidationError {
        Optional<String> msg = getLogMessage(logMessage);
        ActionManager.EventContext action = new ActionManager(
                framedGraph, permissionScope).newEventContext(actioner,
                EventTypes.ingest, msg);
        ImportLog log = new ImportLog(msg.orElse(null));

        ArchiveEntry entry;
        for (int i = 1; (entry = stream.getNextEntry()) != null; i++) {
            try {
                if (!entry.isDirectory()) {
                    currentFile = entry.getName();
                    BoundedInputStream boundedInputStream = new BoundedInputStream(stream, entry.getSize());
                    boundedInputStream.setPropagateClose(false);
                    logger.info("Importing file {}: {}", i, currentFile);
                    importInputStream(boundedInputStream, currentFile, action, log);
                }
            } catch (ValidationError e) {
                log.addError(formatErrorLocation(), e.getMessage());
                if (!options.tolerant) {
                    throw new ImportValidationError(formatErrorLocation(), e);
                }
            } catch (InputParseError e) {
                log.addError(formatErrorLocation(), e.getMessage());
                if (!options.tolerant) {
                    throw e;
                }
            }
        }

        // Only mark the transaction successful if we're
        // actually accomplished something.
        return log.committing(action);
    }

    /**
     * Import an InputStream with an event context.
     *
     * @param stream  the InputStream to import
     * @param tag     an optional tag identifying the source of the stream
     * @param context the event that this import is part of
     * @param log     an import log to which the result will be written
     * @throws IOException     if the input stream cannot be read
     * @throws ValidationError if data constraints are not met
     * @throws InputParseError if the input stream cannot be parsed
     */
    protected abstract void importInputStream(InputStream stream,
                                              String tag,
                                              ActionManager.EventContext context,
                                              ImportLog log)
            throws IOException, ValidationError, InputParseError;

    /**
     * A default handler for import callbacks which adds the item to the
     * log and event context.
     *
     * @param log      an import log
     * @param tag      an identifier for the import source
     * @param context  an event context
     * @param mutation the item mutation
     */
    void defaultImportCallback(ImportLog log, String tag, ActionManager.EventContext context, Mutation<? extends Accessible> mutation) {
        String id = mutation.getNode().getId();
        switch (mutation.getState()) {
            case CREATED:
                logger.info("Item created: {}", id);
                context.addSubjects(mutation.getNode());
                log.addCreated(tag, id);
                break;
            case UPDATED:
                if (!options.updates) {
                    throw new ModeViolation(String.format(
                            "Item '%s' was updated but import manager does not allow updates",
                            id));
                }
                logger.info("Item updated: {}", id);
                context.addSubjects(mutation.getNode());
                log.addUpdated(tag, id);
                break;
            default:
                log.addUnchanged(tag, id);
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
        return String.format("File: %s, XML document: %d", currentFile, currentPosition);
    }

    private InputStream readUrl(URL url, int retry) throws IOException {
        // EXTREMELY INADVISABLE CODE! This stuff runs inside the database
        // where Thread.sleep()ing is obviously not a good idea. However,
        // since import errors caused by HTTP rate-limiting are generally
        // very rare I've resorted to it here with exponential backoff as
        // by far the easiest way to prevent sporadic import problems.
        // Generally we only need one retry with a 100ms sleep per several
        // thousand import URLs to make HTTP imports much more resilient,
        // and very few dataset batches have that many items anyway.
        try {
            InputStream stream = url.openStream();
            consecutiveIoErrors = 0;
            return stream;
        } catch (IOException e) {
            if (isSlowDownError(e) && retry < MAX_RETRIES) {
                logger.debug("Got slow down error... retry {}, {}", retry + 1, url);
                consecutiveIoErrors++;
                try {
                    Thread.sleep((long) (Math.pow(2, retry) * 100));
                    return readUrl(url, retry + 1);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw e;
        }
    }

    private boolean isSlowDownError(IOException e) {
        return e.getMessage().contains("Server returned HTTP response code: 503");
    }
}
