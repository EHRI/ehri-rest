package eu.ehri.project.importers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
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
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Class that provides a front-end for importing EAD XML files, EADGRP, and
 * nested lists of EAD documents into the graph.
 * 
 * @author michaelb
 * 
 */
public class EadImportManager {

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
     * Import an EAD via an URL.
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param address
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     */
    public Action importUrl(String logMessage, String address)
            throws IOException, SAXException, ValidationError {
        URL url = new URL(address);
        InputStream ios = url.openStream();
        try {
            return importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    /**
     * Import an EAD file by specifying it's path.
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param filePath
     * @throws IOException
     * @throws SAXException
     * @throws ValidationError
     */
    public Action importFile(String logMessage, String filePath)
            throws IOException, SAXException, ValidationError {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(logMessage, ios);
        } finally {
            ios.close();
        }
    }

    /**
     * Import EAD from the given InputStream.
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param ios
     * @throws SAXException
     * @throws IOException
     * @throws ValidationError
     */
    public Action importFile(String logMessage, InputStream ios)
            throws SAXException, IOException, ValidationError {

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
            importWithAction(action, doc);
            tx.success();
            return action;
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
     * 
     * @param graph
     * @param agent
     * @param actioner
     * @param logMessage
     * @param paths
     * @throws SAXException
     * @throws IOException
     * @throws ValidationError
     */
    public Action importFiles(String logMessage, List<String> paths)
            throws SAXException, IOException, ValidationError {

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
            Integer count = 0;
            for (String path : paths) {
                FileInputStream ios = new FileInputStream(path);
                try {
                    logger.info("Importing file: " + path);
                    count += importWithAction(action, builder.parse(ios));
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
    private Integer importWithAction(final Action action, Document doc)
            throws ValidationError, InvalidEadDocument, InvalidInputDataError {
        
        // Check the various types of document we support. This
        // includes <eadgrp> or <eadlist> types.
        if (doc.getDocumentElement().getNodeName().equals("ead")) {
            return importWithAction(action, (Node)doc.getDocumentElement());
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
    private Integer importNestedItems(final Action action, Document doc,
            String path) throws ValidationError, InvalidInputDataError {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList eadList;
        try {
            eadList = (NodeList)xpath.compile(path).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for (int i = 0; i < eadList.getLength(); i++) {
            try {
                return importWithAction(action, eadList.item(i));
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
        return 0;
    }
    
    /**
     * Import a Node doc using the given action.
     * 
     * @param action
     * @param doc
     * @return 
     * @throws ValidationError 
     * @throws InvalidInputDataError 
     */
    private Integer importWithAction(final Action action, Node doc) throws ValidationError, InvalidInputDataError {
        EadImporter importer = new EadImporter(framedGraph, agent, doc);
        final Map<String,Integer> counter = new HashMap<String,Integer>();
        counter.put("counter", 0);
        // Create a new action for this import
        importer.addCreationCallback(new CreationCallback() {
            public void itemImported(AccessibleEntity item) {
                action.addSubjects(item);
                counter.put("counter", counter.get("count") + 1);
            }
        });
        importer.importItems();
        return counter.get("counter");
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        final String logMessage = "Imported from command-line";

        Options options = new Options();
        options.addOption(new Option("createrepo", false,
                "Create agent with the given ID"));
        options.addOption(new Option("repo", true,
                "Identifier of repository to import into"));
        options.addOption(new Option("createuser", false,
                "Create user with the given ID"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false,
                "Don't error if a file is not valid."));

        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(options, args);

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(
                    "Usage: importer [OPTIONS] -user <user-id> -repo <agent-id> <neo4j-graph-dir> <ead1.xml> <ead2.xml> ... <eadN.xml>");

        // Get the graph and search it for the required agent...
        FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                new Neo4jGraph((String) cmdLine.getArgList().get(0)));

        List<String> filePaths = new LinkedList<String>();
        for (int i = 1; i < cmdLine.getArgList().size(); i++) {
            filePaths.add((String) cmdLine.getArgList().get(i));
        }

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Agent agent = null;
            if (cmdLine.hasOption("createrepo")) {
                agent = createAgent(graph, cmdLine.getOptionValue("repo"));
            } else {
                agent = graph
                        .getVertices("identifier",
                                (String) cmdLine.getArgList().get(0),
                                Agent.class).iterator().next();
            }
            UserProfile user = null;
            if (cmdLine.hasOption("createuser")) {
                user = createUser(graph, cmdLine.getOptionValue("user"));
            } else {
                user = graph
                        .getVertices("identifier",
                                (String) cmdLine.getArgList().get(0),
                                UserProfile.class).iterator().next();
            }

            EadImportManager manager = new EadImportManager(graph, agent, user);
            manager.setTolerant(cmdLine.hasOption("tolerant"));
            Action action = manager.importFiles(logMessage, filePaths);

            int itemCount = 0;
            for (@SuppressWarnings("unused")
            AccessibleEntity ent : action.getSubjects()) {
                itemCount++;
            }

            System.out.println("Imported item count: " + itemCount);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            tx.failure();
        } finally {
            tx.finish();
            graph.shutdown();
        }
    }

    private static UserProfile createUser(FramedGraph<Neo4jGraph> graph,
            String name) throws ValidationError {
        Map<String, Object> agentData = new HashMap<String, Object>();
        agentData.put("userId", name);
        agentData.put("name", name);
        EntityBundle<UserProfile> agb = new BundleFactory<UserProfile>()
                .buildBundle(agentData, UserProfile.class);
        return new BundleDAO<UserProfile>(graph).create(agb);
    }

    private static Agent createAgent(FramedGraph<Neo4jGraph> graph, String name)
            throws ValidationError {
        Map<String, Object> agentData = new HashMap<String, Object>();
        agentData.put("identifier", name);
        agentData.put("name", name);
        EntityBundle<Agent> agb = new BundleFactory<Agent>().buildBundle(
                agentData, Agent.class);
        return new BundleDAO<Agent>(graph).create(agb);
    }
}
