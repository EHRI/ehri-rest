package eu.ehri.project.importers.cvoc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidEadDocument;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;

/**
 * Importer for the controled vocabulary (TemaTres thesaurus) 
 * in a SKOS-Core RDF file. 
 * 
 * This is different from the other importing mainly because I don't fully understand that. 
 * So this importer will also do the 'management' and we will see later on how to refactor it. 
 *
 * Also note that: We don't have an Agent for the CVOCs!
 *  
 * @author paulboon
 *
 */
public class SkosCoreCvocImporter {
	   private static final Logger logger = LoggerFactory
	            .getLogger(SkosCoreCvocImporter.class);

	// most stuff copied from the EadImportManager and its base classes
	// but we don't have EAD and 
	// note that we don't have an Agent for the CVOCs!
	
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final Actioner actioner;
    protected Boolean tolerant = false;

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
     * Constructor.
     * 
     * @param framedGraph
     * @param actioner
     */
    public SkosCoreCvocImporter(FramedGraph<Neo4jGraph> framedGraph,
             final Actioner actioner) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
    }
    
    /*** management part ***/

	public ImportLog importFile(String filePath, String logMessage)
	            throws IOException, InputParseError, ValidationError {
	        FileInputStream ios = new FileInputStream(filePath);
	        try {
	            return importFile(ios, logMessage);
	        } finally {
	            ios.close();
	        }
	    }
	
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
	
    private void importFile(InputStream ios, final Action action,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidEadDocument, InvalidInputFormatError, IntegrityError {

        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(!tolerant);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (tolerant)
                builder.setEntityResolver(new DummyEntityResolver());
            Document doc = builder.parse(ios);
            importDocWithinAction(doc, action, log);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new InputParseError(e);
        }

    }
    
    /*** End of management part ***/
    
    /**
     * Import an XML doc using the given action.
     * 
     * @param doc
     * @param action
     * 
     * @return
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError 
     */
    private void importDocWithinAction(Document doc, final Action action,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError, IntegrityError {

    	// Do the Concept extraction from the XML
    	// but how to use the namespacing properly?
    	// for now suppose the root node is rdf:RDF
    	 if (doc.getDocumentElement().getNodeName().equals("rdf:RDF")) {
             Element rdfElement = doc.getDocumentElement();
             // get all concepts, ehhh without XPath
             NodeList nodeList = rdfElement.getElementsByTagName("rdf:Description");
             for(int i=0; i<nodeList.getLength(); i++){
            	  Node childNode = nodeList.item(i);
            	  Element element = (Element) childNode; // it should be!
            	  
            	  if (isConceptElement(element)) {
            		 // extract ... data for concept 
            		 // in Map<String, Object>
            		 // then use BundleDAO.createOrUpdate and use the framed Entity = CvocConcept  
            		 // see: AbstractImporter.importItem
            		  
             		 // persist concept
            		 Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT,
            				  extractCvocConcept(element));
            		 // get an ID for the GraphDB
            		 IdGenerator generator = AccessibleEntityIdGenerator.INSTANCE;
            	     String id = null;
            	     /*
            	        try {
            	            id = generator.generateId(EntityClass.CVOC_CONCEPT, scope,
            	                    unit.getData());
            	        } catch (IdGenerationError e) {
            	            throw new ValidationError(unit, Concept.IDENTIFIER_KEY,
            	                    (String) unit.getData().get(Concept.IDENTIFIER_KEY));
            	        }
            	        
            		 BundleDAO persister = new BundleDAO(framedGraph);
            		 Concept frame = persister.createOrUpdate(unit.withId(id),
            				 Concept.class);
            		  
            		 //action.addSubjects(item); // when concept was successfully persisted!
            		 manifest.addCreated();
            		 */
            	  } 
             }
         }
    	
    	 // TODO Next step is to set all relations between the concepts; BT/NT/RT
    }
    
	// but now check if it has an rdf:type element 
	// with the attribute rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"
    private boolean isConceptElement(Element element) {
    	boolean result = false;
    	
    	NodeList nodeList = element.getElementsByTagName("rdf:type");
        for(int i=0; i<nodeList.getLength(); i++){
      	  Node typeNode = nodeList.item(i);
      	  Node namedItem = typeNode.getAttributes().getNamedItem("rdf:resource");
      	  if (namedItem.getNodeValue().contentEquals("http://www.w3.org/2004/02/skos/core#Concept")) {
      		  result = true;
      	  }
        }
        
    	return result;
    }
    
    
    Map<String, Object> extractCvocConcept(Node data)
            throws ValidationError {
        Map<String, Object> dataMap = new HashMap<String, Object>();

        // are we using the rdf:about attribute as 'identifier'
        Node namedItem = data.getAttributes().getNamedItem("rdf:about");
        String value = namedItem.getNodeValue();
        dataMap.put(AccessibleEntity.IDENTIFIER_KEY, value);

        // TODO extract and add the prefLabels and all other CvocText items
        
        logger.info("Importing item: " + dataMap.get(AccessibleEntity.IDENTIFIER_KEY));

        return dataMap;
    }
    
    
    /*** ***/
    
    /**
     * Dummy resolver that does nothing. This is used to ensure that, in
     * tolerant mode, the EAD parser does not lookup the DTD and validate the
     * document, which is both slow and error prone.
     */
    public class DummyEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicID, String systemID)
                throws SAXException {

            return new InputSource(new StringReader(""));
        }
    }

}
