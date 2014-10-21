package eu.ehri.project.importers;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Class that provides a front-end for importing XML files like EAD and EAC and
 * nested lists of EAD documents into the graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SaxImportManager extends XmlImportManager implements ImportManager {

    private static final Logger logger = LoggerFactory.getLogger(SaxImportManager.class);
    private AbstractImporter<Map<String, Object>> importer;
    private Class<? extends AbstractImporter> importerClass;
    Class<? extends SaxXmlHandler> handlerClass;
    private XmlImportProperties properties;
    private VirtualUnit virtualcollection;

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
        logger.info("importer used: " + importerClass);
        logger.info("handler used: " + handlerClass);
    }
    /**
     * Constructor.
     *
     * @param framedGraph
     * @param permissionScope
     * @param actioner
     */
    public SaxImportManager(FramedGraph<? extends TransactionalGraph> framedGraph,
            final PermissionScope permissionScope, final Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass, XmlImportProperties properties) {
        this(framedGraph, permissionScope, actioner, importerClass, handlerClass);
        this.properties=properties;
    }
//
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
            
            
            importer.addCreationCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    logger.info("Item created: {}", item.getId());
                    eventContext.addSubjects(item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                public void itemImported(AccessibleEntity item) {
                    logger.info("Item updated: {}", item.getId());
                    eventContext.addSubjects(item);
                    log.addUpdated();
                }
            });
            importer.addUnchangedCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    log.addUnchanged();
                }
            });
            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
            SaxXmlHandler handler;
            if(properties == null){
              handler = handlerClass.getConstructor(AbstractImporter.class).newInstance(importer);
            }else{
              handler = handlerClass.getConstructor(AbstractImporter.class, XmlImportProperties.class).newInstance(importer, properties);
                
            }

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            if (isTolerant()) {
                logger.debug("Turning off validation and setting schema to null");
                spf.setValidating(false);
                spf.setSchema(null);
            }
            logger.debug("isValidating: " + spf.isValidating());
            SAXParser saxParser = spf.newSAXParser();
            saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
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

    public SaxImportManager setProperties(XmlImportProperties properties) {
        return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass, properties);
    }

    public SaxImportManager setProperties(String properties) {
        if (properties == null) {
            return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass, null);
        } else {
            XmlImportProperties xmlImportProperties = new XmlImportProperties(properties);
            return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass, xmlImportProperties);
        }
    }
    
    public void setVirtualCollection(VirtualUnit virtualcollection) {
        this.virtualcollection=virtualcollection;
    }
}
