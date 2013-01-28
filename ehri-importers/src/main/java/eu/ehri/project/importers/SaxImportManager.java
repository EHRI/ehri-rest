package eu.ehri.project.importers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidEadDocument;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;

/**
 * Class that provides a front-end for importing XML files like EAD and EAC and
 * nested lists of EAD documents into the graph.
 *
 * @author michaelb
 *
 */
public class SaxImportManager extends XmlImportManager implements ImportManager {

    private static final Logger logger = LoggerFactory
            .getLogger(SaxImportManager.class);
    private Boolean tolerant = false;
    private XmlCVocImporter importer; // CVoc specific!
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final Agent agent;
    protected final Actioner actioner;
    protected final ActionManager actionManager;
    
    // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    private String currentFile = null;
    private Integer currentPosition = null;

    private  Class<? extends XmlCVocImporter> importerClass; // CVoc specific!
    Class<? extends SaxXmlHandler> handlerClass;
    /**
     * Dummy resolver that does nothing. This is used to ensure that, in
     * tolerant mode, the parser does not lookup the DTD and validate the
     * document, which is both slow and error prone.
     */
    public class DummyEntityResolver implements EntityResolver {

        public InputSource resolveEntity(String publicID, String systemID)
                throws SAXException {

            return new InputSource(new StringReader(""));
        }
    }

    /**
     * Constructor.
     *
     * @param framedGraph
     * @param agent
     * @param actioner
     */
    public SaxImportManager(FramedGraph<Neo4jGraph> framedGraph,
            final Agent agent, final Actioner actioner, Class<? extends XmlCVocImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        this.framedGraph = framedGraph;
        this.agent = agent;
        this.actioner = actioner;
        this.actionManager = new ActionManager(framedGraph);
        this.importerClass = importerClass;
        this.handlerClass = handlerClass;
        
    }

    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     *
     * @param tolerant
     */
    public void setTolerant(Boolean tolerant) {
        logger.info("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;
    }

    /**
     * Import a file, creating a new action with the given log message.
     *
     * @param ios
     * @param logMessage
     * @return
     *
     * @throws IOException
     * @throws ValidationError
     * @throws InvalidEadDocument
     * @throw InputParseError
     */
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {
        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {
            // Create a new action for this import
            final Action action = new ActionManager(framedGraph).createAction(
                    actioner, logMessage);
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(action);

            // Do the import...
            importFile(ios, action, log);
            // If nothing was imported, remove the action...
            if (log.isValid()) {
                tx.success();
            }

            return log;
        } catch (ValidationError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tx.finish();
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
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError {

        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {

            final Action action = new ActionManager(framedGraph).createAction(
                    actioner, logMessage);
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
                } catch (InvalidEadDocument e) {
                    log.setErrored(formatErrorLocation(), e.getMessage());
                    if (!tolerant) {
                        throw e;
                    }
                }
            }

            // Only mark the transaction successful if we're
            // actually accomplished something.
            if (log.isValid()) {
                tx.success();
            }

            return log;
        } catch (ValidationError e) {
            tx.failure();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Import EAD from the given InputStream, as part of the given action.
     *
     * @param ios
     * @param action
     * @param log
     *
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidInputFormatError
     * @throws InvalidEadDocument
     */
    private void importFile(InputStream ios, final Action action,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidEadDocument, InvalidInputFormatError {

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            importer = importerClass.getConstructor(FramedGraph.class, Agent.class, ImportLog.class).newInstance(framedGraph, agent, log);
            importer.addCreationCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    System.out.println("ImportCallback: itemImported creation " + item.getIdentifier());
                    actionManager.addSubjects(action, actioner, item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    System.out.println("ImportCallback: itemImported updated");
                    actionManager.addSubjects(action, actioner, item);
                    log.addUpdated();
                }
            });
            
            // Note: CVoc specific !
            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
            DefaultHandler handler = handlerClass.getConstructor(AbstractCVocImporter.class).newInstance(importer); 
            saxParser.parse(ios, handler); //TODO + log
            
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            java.util.logging.Logger.getLogger(SaxImportManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new InputParseError(e);
        }

    }

    private String formatErrorLocation() {
        return String.format("File: %s, EAD document: %d", currentFile,
                currentPosition);
    }
    
}
