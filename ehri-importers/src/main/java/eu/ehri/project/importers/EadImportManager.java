package eu.ehri.project.importers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidEadDocument;
import eu.ehri.project.importers.exceptions.InvalidInputDataError;
import eu.ehri.project.importers.exceptions.NoItemsCreated;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;

/**
 * Class that provides a front-end for importing EAD XML files, EADGRP, and
 * nested lists of EAD documents into the graph.
 * 
 * @author michaelb
 * 
 */
public class EadImportManager extends XmlImportManager implements ImportManager {

    private static final String EADGRP_PATH = "//eadgrp/archdescgrp/dscgrp/ead";

    private static final String EADLIST_PATH = "//eadlist/ead";

    private static final Logger logger = LoggerFactory
            .getLogger(EadImportManager.class);
    
    private Boolean tolerant = false;

    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final Agent agent;
    protected final Actioner actioner;

    /**
     * Constructor.
     * 
     * @param framedGraph
     * @param agent
     * @param actioner
     */
    public EadImportManager(FramedGraph<Neo4jGraph> framedGraph,
            final Agent agent, final Actioner actioner) {
        this.framedGraph = framedGraph;
        this.agent = agent;
        this.actioner = actioner;
    }
    
    /**
     * Tell the importer to simply skip invalid items
     * rather than throwing an exception.
     * 
     * @param tolerant
     */
    public void setTolerant(Boolean tolerant) {
        logger.info("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;                
    }
    
    /**
     * Helper class for counting within a closure.
     * @author michaelb
     *
     */
    class ItemCounter {
        public int count = 0;        
    }

    /**
     * Import EAD from the given InputStream.
     * @param ios
     * @param logMessage
     * @param graph
     * @param agent
     * @param actioner
     * 
     * @throws SAXException
     * @throws IOException
     * @throws ValidationError
     */
    public Action importFile(InputStream ios, String logMessage)
            throws SAXException, IOException, ValidationError, NoItemsCreated {

        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Document doc = builder.parse(ios);
        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {
            // Create a new action for this import
            final Action action = new ActionManager(framedGraph).createAction(
                    actioner, logMessage);
            if (importDocWithAction(action, doc) == 0)
                throw new NoItemsCreated();
            tx.success();
            return action;
        } catch (NoItemsCreated e) {
            tx.failure();
            throw e;
        } catch (ValidationError e) {
            tx.failure();
            throw e;
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Import multiple files in the same batch/transaction.
     * @param paths
     * @param logMessage
     * @param graph
     * @param agent
     * @param actioner
     * 
     * @throws SAXException
     * @throws IOException
     * @throws ValidationError
     * @throws NoItemsCreated 
     */
    public Action importFiles(List<String> paths, String logMessage)
            throws SAXException, IOException, ValidationError, NoItemsCreated {

        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        Transaction tx = framedGraph.getBaseGraph().getRawGraph().beginTx();
        try {

            final Action action = new ActionManager(framedGraph).createAction(
                    actioner, logMessage);
            int count = 0;
            for (String path : paths) {
                FileInputStream ios = new FileInputStream(path);
                try {
                    logger.info("Importing file: " + path);
                    count += importDocWithAction(action, builder.parse(ios));
                } finally {
                    ios.close();
                }
            }
            
            // If nothing was created we don't want the pointless
            // action hanging around, so barf...
            if (count == 0)
                throw new NoItemsCreated();            
            
            tx.success();
            return action;
        } catch (NoItemsCreated e) {
            tx.failure();
            throw e;
        } catch (ValidationError e) {
            tx.failure();
            throw e;
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException(e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Import an XML doc using the given action.
     * 
     * @param action
     * @param doc
     * @return 
     * @throws ValidationError
     * @throws InvalidEadDocument 
     * @throws InvalidInputDataError 
     */
    private int importDocWithAction(final Action action, Document doc)
            throws ValidationError, InvalidEadDocument, InvalidInputDataError {
        
        // Check the various types of document we support. This
        // includes <eadgrp> or <eadlist> types.
        if (doc.getDocumentElement().getNodeName().equals("ead")) {
            return importNodeWithAction(action, (Node)doc.getDocumentElement());
        } else if (doc.getDocumentElement().getNodeName().equals("eadlist")) {
            return importNestedItems(action, doc, EADLIST_PATH);
        } else if (doc.getDocumentElement().getNodeName().equals("eadgrp")) {
            return importNestedItems(action, doc, EADGRP_PATH);            
        } else {
            throw new InvalidEadDocument(doc.getDocumentElement().getNodeName());
        }
    }

    /**
     * @param action
     * @param doc
     * @param path
     * @return 
     * @throws ValidationError
     * @throws InvalidInputDataError 
     */
    private int importNestedItems(final Action action, Document doc,
            String path) throws ValidationError, InvalidInputDataError {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList eadList;
        try {
            eadList = (NodeList)xpath.compile(path).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        int count = 0;
        for (int i = 0; i < eadList.getLength(); i++) {
            try {
                count += importNodeWithAction(action, eadList.item(i));
            } catch (InvalidInputDataError e) {
                logger.error(e.getMessage());
                if (!tolerant)
                    throw e;
            } catch (ValidationError e) {
                logger.error(e.getMessage());
                if (!tolerant)
                    throw e;
            }
        }
        return count;
    }
    
    /**
     * Import a Node doc using the given action.
     * 
     * @param action
     * @param node
     * @return 
     * @throws ValidationError 
     * @throws InvalidInputDataError 
     */
    private int importNodeWithAction(final Action action, Node node) throws ValidationError, InvalidInputDataError {
    	
        EadImporter importer = new EadImporter(framedGraph, agent, node);
        final ItemCounter counter = new ItemCounter();
        // Create a new action for this import
        importer.addCreationCallback(new CreationCallback() {
            public void itemImported(AccessibleEntity item) {
                action.addSubjects(item);
                counter.count += 1;
            }
        });
        try {
            importer.importItems();
        } catch (InvalidInputDataError e) {
            logger.error(e.getMessage());
            if (!tolerant)
                throw e;
        } catch (ValidationError e) {
            logger.error(e.getMessage());
            if (!tolerant)
                throw e;
        }
        return counter.count;
    }
}
