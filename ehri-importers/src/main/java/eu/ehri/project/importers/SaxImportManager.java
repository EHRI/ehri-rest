package eu.ehri.project.importers;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Class that provides a front-end for importing XML files like EAD and EAC and
 * nested lists of EAD documents into the graph.
 *
 * @author michaelb
 */
public class SaxImportManager extends XmlImportManager implements ImportManager {

    private static final Logger logger = LoggerFactory.getLogger(SaxImportManager.class);
    private AbstractImporter<Map<String, Object>> importer;
    private Class<? extends AbstractImporter> importerClass;
    Class<? extends SaxXmlHandler> handlerClass;

    /**
     * Constructor.
     *
     * @param framedGraph
     * @param permissionScope
     * @param actioner
     */
    public SaxImportManager(FramedGraph<? extends TransactionalGraph> framedGraph,
            final PermissionScope permissionScope, final Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        super(framedGraph, permissionScope, actioner);
        this.importerClass = importerClass;
        this.handlerClass = handlerClass;
    }

    public SaxImportManager addCallback(ImportCallback cb) {
        importer.addCallback(cb);
        return this;
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param ios
     * @param eventContext
     * @param log
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidInputFormatError
     * @throws InvalidXmlDocument
     */
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidXmlDocument, InvalidInputFormatError {

        try {
            importer = importerClass.getConstructor(FramedGraph.class, PermissionScope.class,
                    ImportLog.class).newInstance(framedGraph, permissionScope, log);
            logger.debug("importer of class {}", importer.getClass());
            importer.addCallback(new ImportCallback() {
                public void itemImported(Mutation<? extends AccessibleEntity> mutation) {
                    switch (mutation.getState()) {
                        case CREATED:
                            logger.info("Item created: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addCreated();
                            break;
                        case UPDATED:
                            logger.info("Item created: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addCreated();
                            break;
                        case UNCHANGED:
                            log.addUnchanged();
                            break;
                    }
                }
            });

            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
            SaxXmlHandler handler = handlerClass.getConstructor(AbstractImporter.class).newInstance(importer);
            logger.debug("handler of class {}", handler.getClass());

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            if (isTolerant()) {
                logger.debug("Turning off validation and setting schema to null");
                spf.setValidating(false);
                spf.setSchema(null);
            }
            logger.debug("isValidating: " + spf.isValidating());
            SAXParser saxParser = spf.newSAXParser();
            saxParser.parse(ios, handler);
        } catch (InstantiationException ex) {
            logger.error("InstantiationException: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.error("IllegalAccess: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("IllegalArgumentException: " + ex.getMessage());
        } catch (InvocationTargetException ex) {
            logger.error("InvocationTargetException: " + ex.getMessage());
        } catch (NoSuchMethodException ex) {
            logger.error("NoSuchMethodException: " + ex.getMessage());
        } catch (SecurityException ex) {
            logger.error("SecurityException: " + ex.getMessage());
        } catch (ParserConfigurationException ex) {
            logger.error("ParserConfigurationException: " + ex.getMessage());
            throw new RuntimeException(ex);
        } catch (SAXException e) {
            logger.error("SAXException: " + e.getMessage());
            throw new InputParseError(e);
        }
    }
}
