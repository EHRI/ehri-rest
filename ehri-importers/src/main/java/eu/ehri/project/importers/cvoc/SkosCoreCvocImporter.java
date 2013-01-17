package eu.ehri.project.importers.cvoc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
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

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
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
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.GenericIdGenerator;
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
    protected final Vocabulary vocabulary;
    
    // map from the internal Skos identifier to the placeholder
    protected Map<String, ConceptPlaceholder> conceptLookup = new HashMap<String, ConceptPlaceholder>();
    
    /**
     * Constructor.
     * 
     * @param framedGraph
     * @param actioner
     */
    public SkosCoreCvocImporter(FramedGraph<Neo4jGraph> framedGraph,
             final Actioner actioner, Vocabulary vocabulary) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
    }
    
    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     * 
     * @param tolerant
     */
    public void setTolerant(Boolean tolerant) {
        logger.debug("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;
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

    	 createConcepts(doc, action, manifest);

    	 // All Concepts are created, now the next step 
    	 createVocabularyStruture(doc, action, manifest);
    }

	// Do the Concept data extraction and create the Concepts in the db
    private void createConcepts(Document doc, final Action action,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError, IntegrityError {

    	// the Concept is extracted from the XML 
    	// but how to use the namespacing properly?
    	// for now suppose the root node is rdf:RDF
    	 if (doc.getDocumentElement().getNodeName().equals("rdf:RDF")) {
             Element rdfElement = doc.getDocumentElement();
             // get all concepts,  without XPath
             // NOTE the concept can be in a skos:Concept or an rdf:Description
             // for now assume we have RDF !
             NodeList nodeList = rdfElement.getElementsByTagName("rdf:Description");
             for(int i=0; i<nodeList.getLength(); i++){
            	  Node childNode = nodeList.item(i);
            	  Element element = (Element) childNode; // it should be!
            	  
            	  if (isConceptElement(element)) {
            		 // extract ... data for concept 
            		 // in Map<String, Object>
            		 // then use BundleDAO.createOrUpdate and use the framed Entity = CvocConcept  
            		 // see: AbstractImporter.importItem
            		   
            		 Bundle unit = constructBundle(element);
        	        
            		 BundleDAO persister = new BundleDAO(framedGraph);
            		 Concept frame = persister.createOrUpdate(unit,
            				 Concept.class);
            		 
            		 // Set the vocabulary/concept relationship
            		 PermissionScope scope = vocabulary;
            		 frame.setVocabulary(vocabulary);
            		 frame.setPermissionScope(scope);

            		 action.addSubjects(frame); // when concept was successfully persisted!
            		 manifest.addCreated();
            		 
            	     // Create and add a ConceptPlaceholder 
            		 // for making the vocabulary (relation) structure in the next step
            		 List<String> broaderIds = getBroaderConceptIds(element);
            		 logger.debug("Concept has " + broaderIds.size() 
            				 + " broader ids: " + broaderIds.toString());
            		 String storeId = unit.getId();//id;
            		 String skosId = frame.getIdentifier(); // the identifier used in the Skos file and is used for internal referal
            		 logger.debug("Concept store id = " + storeId + ", skos id = " + skosId);
            		 conceptLookup.put(skosId, new ConceptPlaceholder(storeId, broaderIds, frame));
            	  } 
             }
         }    	
    }
    
    // extract data and construct the bundle
    private Bundle constructBundle(Element element) throws ValidationError {
		  Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT,
				  extractCvocConcept(element));
		 
		  // add the description data to the concept as relationship
		  Map<String, Object> descriptions = extractCvocConceptDescriptions(element);
		  for (String key : descriptions.keySet()) {
			  logger.debug("description for: " + key);
			  Map<String, Object> d = (Map<String, Object>)descriptions.get(key);
			  logger.debug("languageCode = " + d.get("languageCode"));

			  // NOTE maybe best if pferLabel is there?

			  unit = unit.withRelation(Description.DESCRIBES, new Bundle(
					  EntityClass.CVOC_CONCEPT_DESCRIPTION, d));
		  }
		  // NOTE the following gives a lot of output!
		  //logger.debug("Bundle as JSON: \n" + unit.toString());

		  // get an ID for the GraphDB
		  IdGenerator generator = GenericIdGenerator.INSTANCE;//AccessibleEntityIdGenerator.INSTANCE;
		  String id = null;
		  PermissionScope scope = vocabulary;

		  try {
			  id = generator.generateId(EntityClass.CVOC_CONCEPT, scope,
					  unit.getData());
		  } catch (IdGenerationError e) {
			  throw new ValidationError(unit, Concept.IDENTIFIER_KEY,
					  (String) unit.getData().get(Concept.IDENTIFIER_KEY));
		  }

		  unit = unit.withId(id);
		  return unit;
    }
    
    private List<String> getBroaderConceptIds(Element element) {
    	List<String> ids = new ArrayList<String>();
    	
    	NodeList nodeList = element.getElementsByTagName("skos:broader");
        for(int i=0; i<nodeList.getLength(); i++){
        	  Node typeNode = nodeList.item(i);
        	  Node namedItem = typeNode.getAttributes().getNamedItem("rdf:resource");
        	  ids.add(namedItem.getNodeValue());
        }
        
    	return ids;
    }
    
    // Store all relations between the concepts; BT/NT/RT in the database
    // NOTE that we want this to be done in the same database 'transaction' 
    // and this 'current' one is not finished. 
    // Therefore the Concepts are not retrievable from the database yet!
    private void createVocabularyStruture(Document doc, final Action action,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError, IntegrityError {
    	
	   	 logger.debug("Number of concepts in lookup: " + conceptLookup.size());
	   	 
	   	 // check the lookup and start making the BT relations
	   	 // visit all concepts an see if they have broader concepts
	     for (String skosId : conceptLookup.keySet()) {
	    	 ConceptPlaceholder conceptPlaceholder = conceptLookup.get(skosId);
	    	 if (!conceptPlaceholder.broaderIds.isEmpty()) {
	    		 logger.debug("Concept with skos id [" + skosId 
	    				 + "] has broader terms");
	    		 // find them in the lookup
	    		 for (String bsId: conceptPlaceholder.broaderIds) {
	    			 if (conceptLookup.containsKey(bsId)) {
	    				 // found
	    				 logger.debug("Found mapping from: " + bsId 
	    						 + " to: " + conceptLookup.get(bsId).storeId);
	    				 
	    				 createBNrelation(conceptLookup.get(bsId), conceptPlaceholder);
	    			 } else {
	    				 // not found
	    				 logger.debug("Found NO mapping for: " + bsId); 
	    				 // NOTE What does this mean; refers to an External resource, not in this file?
	    			 }
	    		 }
	    	 }
	     }

	     // TODO check and fix relations
    }
    
    /**
     * Store the Broader to Narrower Concept relation
     * 
     * Note that we cannot use the storeId's and retrieve them from the database. 
     * we need to use the Concept objects from the placeholders in the lookup. 
     */
    private void createBNrelation(ConceptPlaceholder bc, ConceptPlaceholder nc)  {
    	logger.debug("Storing Broader: " + bc.storeId + " to Narrower: " + nc.storeId);
    	bc.concept.addNarrowerConcept(nc.concept);
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
    
    Map<String, Object> extractCvocConcept(Node node)
            throws ValidationError {
        Map<String, Object> dataMap = new HashMap<String, Object>();

        // are we using the rdf:about attribute as 'identifier'
        Node namedItem = node.getAttributes().getNamedItem("rdf:about");
        String value = namedItem.getNodeValue();
        dataMap.put(AccessibleEntity.IDENTIFIER_KEY, value);

        logger.debug("Extracting Concept id: " + dataMap.get(AccessibleEntity.IDENTIFIER_KEY));        
        
        return dataMap;
    }

    Map<String, Object> extractCvocConceptDescriptions(Element conceptElement) {
        // extract and process the textual items (with a language)
        // one description for each language, so the languageCode serve as a key into the map
        Map<String, Object> descriptionData = new HashMap<String, Object>(); 
        
        // one and only one
        extractAndAddSingleValuedTextToDescriptionData(descriptionData, 
        		"prefLabel", "skos:prefLabel", conceptElement);
        // multiple alternatives is logical
        extractAndAddMultiValuedTextToDescriptionData(descriptionData, 
        		"altLabel", "skos:altLabel", conceptElement);
        // just allow multiple, its not forbidden by Skos
        extractAndAddMultiValuedTextToDescriptionData(descriptionData, 
        		"scopeNote", "skos:scopeNote", conceptElement);
        // just allow multiple, its not forbidden by Skos
        extractAndAddMultiValuedTextToDescriptionData(descriptionData, 
        		"definition", "skos:definition", conceptElement);
        
        // NOTE we could try to also add everything else, using the skos tagname as a key?
        
    	return descriptionData;
    }

    private void extractAndAddSingleValuedTextToDescriptionData(Map<String, Object> descriptionData, 
    		String textName, String skosName, Element conceptElement) {

       	NodeList textNodeList = conceptElement.getElementsByTagName(skosName);
        for(int i=0; i<textNodeList.getLength(); i++){
        	  Node textNode = textNodeList.item(i);
        	  // get lang attribute, we must have that!
        	  Node langItem = textNode.getAttributes().getNamedItem("xml:lang");
        	  String lang = langItem.getNodeValue();
        	  // get value
        	  String text = textNode.getTextContent();
        	  logger.debug("text: \"" + text + "\" lang: \"" + lang + "\"" + ", skos name: " + skosName);

        	  // add to descriptionData
        	  Map<String, Object> d = getOrCreateDescriptionForLanguage(descriptionData, lang);
    		  d.put(textName, text); // only one item with this name per description
        }    	
    }

    private void extractAndAddMultiValuedTextToDescriptionData(Map<String, Object> descriptionData, 
    		String textName, String skosName, Element conceptElement) {

       	NodeList textNodeList = conceptElement.getElementsByTagName(skosName);
        for(int i=0; i<textNodeList.getLength(); i++){
        	  Node textNode = textNodeList.item(i);
        	  // get lang attribute, we must have that!
        	  Node langItem = textNode.getAttributes().getNamedItem("xml:lang");
        	  String lang = langItem.getNodeValue();
        	  // get value
        	  String text = textNode.getTextContent();
        	  logger.debug("text: \"" + text + "\" lang: \"" + lang + "\"" + ", skos name: " + skosName);
        	  // add to descriptionData
        	  Map<String, Object> d = getOrCreateDescriptionForLanguage(descriptionData, lang);
    		  // get the array if it is there, otherwise create it first
    		  //d.put(textName, text); // only one item with this name per description
    		  if (d.containsKey(textName)) {
    			  // should be a list, add it
    			  ((List<String>)d.get(textName)).add(text);
    		  } else {
    			  // create a list first
    			  List<String > textList = new ArrayList<String>();
    			  textList.add(text);
    			  d.put(textName, textList);
    		  }
        }    	
    }    

    private Map<String, Object> getOrCreateDescriptionForLanguage(Map<String, Object> descriptionData, String lang) {
    	Map<String, Object> d = null;
    	if (descriptionData.containsKey(lang)) {
    		d = (Map<String, Object>) descriptionData.get(lang);
    	} else {
    		// create one
    		d = new HashMap<String, Object>();
    		d.put("languageCode", lang); // initialize
    		descriptionData.put(lang, d);
    	}
    	return d;
    }
        
    /*** ***/
    
    /**
     * Used in the lookup
     * 
     * @author paulboon
     *
     */
    private class ConceptPlaceholder {
    	public String storeId; // the identifier used for storage and referring in the repository
    	List<String> broaderIds;
    	Concept concept;
    	
		public ConceptPlaceholder(String storeId, List<String> broaderIds, Concept concept) {
			this.storeId = storeId;
			this.broaderIds = broaderIds;
			
			// NOTE if we have the concept; why do we need those other members?
			this.concept = concept;
		}    	
    }
    
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
