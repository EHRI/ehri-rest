package eu.ehri.project.importers;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

abstract public class XmlImportManager implements ImportManager {
    private static final Logger logger = LoggerFactory.getLogger(XmlImportManager.class);
    protected final FramedGraph<? extends TransactionalGraph> framedGraph;
    protected final PermissionScope permissionScope;
    protected final Actioner actioner;
    private boolean tolerant = false;

        // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    private String currentFile = null;
    private Integer currentPosition = null;


        /**
     * Constructor.
     *
     * @param framedGraph
     * @param permissionScope
     * @param actioner
     */
    public XmlImportManager(FramedGraph<? extends TransactionalGraph> framedGraph, final PermissionScope permissionScope,
            final Actioner actioner) {
        this.framedGraph = framedGraph;
        this.permissionScope = permissionScope;
        this.actioner = actioner;
    }
    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     *
     * @param tolerant true means it won't validateData the xml file
     */
    public XmlImportManager setTolerant(boolean tolerant) {
        logger.info("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;
        return this;
    }

    public boolean isTolerant(){
        return tolerant;
    }

    
    /**
     * Import an Description file by specifying it's path.
     * 
     * @param filePath
     * @param logMessage
     *
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    /**
     * Import a file, creating a new action with the given log message.
     *
     * @param ios
     * @param logMessage
     * @return returns an ImportLog for the given InputStream
     *
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     */
     @Override
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {
        try {
            // Create a new action for this import
            final ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).logEvent(
                        actioner, EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(action);

            // Do the import...
            importFile(ios, action, log);
            // If nothing was imported, remove the action...
            commitOrRollback(log.hasDoneWork());

            return log;
        } catch (ValidationError e) {
            commitOrRollback(false);
            throw e;
        } catch (Exception e) {
            commitOrRollback(false);
            throw new RuntimeException(e);
        }
    }    
     


    /**
     * Import multiple files in the same batch/transaction.
     *
     * @param paths
     * @param logMessage
     *
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError {

        try {

            final ActionManager.EventContext action = new ActionManager(
                    framedGraph, permissionScope).logEvent(
                        actioner, EventTypes.ingest, getLogMessage(logMessage));
            final ImportLog log = new ImportLog(action);
            for (String path : paths) {
                try {
                    currentFile = path;
                    FileInputStream ios = new FileInputStream(path);
                    try {
                        logger.info("Importing file: " + path);
                        importFile(ios, action, log);
                    } finally {
                        ios.close();
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
            commitOrRollback(log.hasDoneWork());

            return log;
        } catch (ValidationError e) {
            commitOrRollback(false);
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            commitOrRollback(false);
            throw new RuntimeException(e);
        }
    }

    private String formatErrorLocation() {
        return String.format("File: %s, XML document: %d", currentFile,
                currentPosition);
    }

    protected abstract void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidXmlDocument, InvalidInputFormatError;

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }

    private void commitOrRollback(boolean okay) {
        TransactionalGraph baseGraph = framedGraph.getBaseGraph();
        if (baseGraph instanceof TxCheckedNeo4jGraph) {
            TxCheckedNeo4jGraph graph = (TxCheckedNeo4jGraph) baseGraph;
            if (!okay && graph.isInTransaction()) {
                graph.rollback();
            }
        } else {
            if (okay)
                baseGraph.commit();
            else
                baseGraph.rollback();
        }
    }
}
