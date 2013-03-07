package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidEadDocument;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
//    private XmlCVocImporter importer; // CVoc specific!
    private AbstractImporter<Map<String, Object>> importer;
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final Agent agent;
    protected final Actioner actioner;
    
    // Ugly stateful variables for tracking import state
    // and reporting errors usefully...
    private String currentFile = null;
    private Integer currentPosition = null;

    private  Class<? extends AbstractImporter> importerClass; 
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
            final Agent agent, final Actioner actioner, Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        this.framedGraph = framedGraph;
        this.agent = agent;
        this.actioner = actioner;
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
     * @throws InputParseError
     */
    @Override
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {
        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {
            // Create a new action for this import
            final ActionManager.EventContext action = new ActionManager(framedGraph).logEvent(
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
    @Override
    public ImportLog importFiles(List<String> paths, String logMessage)
            throws IOException, ValidationError {

        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {

            final ActionManager.EventContext action = new ActionManager(framedGraph).logEvent(
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
    protected int getNodeCount(FramedGraph<Neo4jGraph> graph) {
        return toList(GlobalGraphOperations
                .at(graph.getBaseGraph().getRawGraph()).getAllNodes()).size();
    }
    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext())
            lst.add(it.next());
        return lst;
    }

    /**
     * Import EAD from the given InputStream, as part of the given action.
     *
     * @param ios
     * @param eventContext
     * @param log
     *
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidInputFormatError
     * @throws InvalidEadDocument
     */
    private void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidEadDocument, InvalidInputFormatError {

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            importer = importerClass.getConstructor(FramedGraph.class, Agent.class, ImportLog.class).newInstance(framedGraph, agent, log);
            logger.error("importer of class " + importer.getClass());
            importer.addCreationCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    logger.error("ImportCallback: itemImported creation " + item.getIdentifier());
                    eventContext.addSubjects(item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    logger.error("ImportCallback: itemImported updated");
                    eventContext.addSubjects(item);
                    log.addUpdated();
                }
            });
            // Note: CVoc specific !
            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
//            DefaultHandler handler = handlerClass.getConstructor(AbstractCVocImporter.class).newInstance(importer); 
            DefaultHandler handler = handlerClass.getConstructor(AbstractImporter.class).newInstance(importer); 
            logger.error("handler of class " + handler.getClass());
            saxParser.parse(ios, handler); //TODO + log
            
        } catch (InstantiationException ex) {
            logger.error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.error(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error(ex.getMessage());
        } catch (InvocationTargetException ex) {
            logger.error(ex.getMessage());
        } catch (NoSuchMethodException ex) {
            logger.error(ex.getMessage());
        } catch (SecurityException ex) {
            logger.error(ex.getMessage());
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        } catch (SAXException e) {
            logger.error(e.getMessage());
            throw new InputParseError(e);
        }

    }

    private String formatErrorLocation() {
        return String.format("File: %s, EAD document: %d", currentFile,
                currentPosition);
    }
    
}
