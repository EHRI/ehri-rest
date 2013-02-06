package eu.ehri.project.importers.cvoc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidEadDocument;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.ActionManager.EventContext;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;

/**
 * Importer for the controlled vocabulary (TemaTres thesaurus) 
 * specified by a SKOS-Core RDF file. 
 * 
 * This is different from the other importing mainly because I don't fully understand that. 
 * So this importer will also do the 'management' and we will see later how to refactor it. 
 *
 * Note: most code was copied from the EadImportManager and its base classes
 * Also note that: We don't have an Agent for the CVOCs, but a Vocabulary instead!
 *  
 * @author paulboon
 *
 */
public class SkosCoreCvocImporter {
	   private static final Logger logger = LoggerFactory
	            .getLogger(SkosCoreCvocImporter.class);
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
     * @param vocabulary
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
            final EventContext eventContext = new ActionManager(framedGraph).logEvent(
                    actioner, logMessage);
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(eventContext);

            // Do the import...
            importFile(ios, eventContext, log);
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
	
    private void importFile(InputStream ios, final ActionManager.EventContext eventContext,
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
            importDocWithinAction(doc, eventContext, log);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new InputParseError(e);
        }

    }
    
    /*** End of management part, Vocabulary/Concept code below ***/
    
    /**
     * Import an RDF/XML doc using the given action.
     * 
     * @param doc
     * @param eventContext
     * 
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError 
     */
    private void importDocWithinAction(Document doc, final ActionManager.EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError, IntegrityError {

    	 createConcepts(doc, eventContext, manifest);
    	 createVocabularyStruture(doc, eventContext, manifest);
    }

    /**
     * Do the Concept data extraction and create all the Concepts 
     * 
     * @param doc
     * @param action
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError
     */
    private void createConcepts(Document doc, final ActionManager.EventContext action,
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
            		   
            		 Bundle unit = constructBundleForConcept(element);
        	        
            		 BundleDAO persister = new BundleDAO(framedGraph);
            		 Concept frame = persister.createOrUpdate(unit,
            				 Concept.class);
            		 
            		 // Set the vocabulary/concept relationship
            		 PermissionScope scope = vocabulary;
            		 frame.setVocabulary(vocabulary);
            		 frame.setPermissionScope(scope);

            		 // when concept was successfully persisted!
            		 action.addSubjects(frame);
            		 manifest.addCreated();
            		 
            	     // Create and add a ConceptPlaceholder 
            		 // for making the vocabulary (relation) structure in the next step
            		 List<String> broaderIds = getBroaderConceptIds(element);
            		 logger.debug("Concept has " + broaderIds.size() 
            				 + " broader ids: " + broaderIds.toString());
            		 List<String> relatedIds = getRelatedConceptIds(element);
            		 logger.debug("Concept has " + relatedIds.size() 
            				 + " related ids: " + relatedIds.toString());
            		 
            		 String storeId = unit.getId();//id;
            		 String skosId = frame.getIdentifier(); // the identifier used in the Skos file and is used for internal referal
            		 logger.debug("Concept store id = " + storeId + ", skos id = " + skosId);
            		 conceptLookup.put(skosId, new ConceptPlaceholder(storeId, broaderIds, relatedIds, frame));
            	  } 
             }
         }    	
    }
    
    /**
     * Extract data and construct the bundle for a new Concept
     * 
     * @param element
     * @return
     * @throws ValidationError
     */
    private Bundle constructBundleForConcept(Element element) throws ValidationError {
		  Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT,
				  extractCvocConcept(element));
		 
		  // add the description data to the concept as relationship
		  Map<String, Object> descriptions = extractCvocConceptDescriptions(element);
		  for (String key : descriptions.keySet()) {
			  logger.debug("description for: " + key);
			  Map<String, Object> d = (Map<String, Object>)descriptions.get(key);
			  logger.debug("languageCode = " + d.get("languageCode"));

			  // NOTE maybe test if prefLabel is there?

			  unit = unit.withRelation(Description.DESCRIBES, new Bundle(
					  EntityClass.CVOC_CONCEPT_DESCRIPTION, d));
		  }
		  // NOTE the following gives a lot of output!
		  //logger.debug("Bundle as JSON: \n" + unit.toString());

		  // get an ID for the GraphDB
		  IdGenerator generator = GenericIdGenerator.INSTANCE;//AccessibleEntityIdGenerator.INSTANCE;
		  PermissionScope scope = vocabulary;

          String id = generator.generateId(EntityClass.CVOC_CONCEPT, scope, unit);
		  unit = unit.withId(id);
		  return unit;
    }
    
    /**
     * Get the list of id's of the Concept's broader concepts
     * 
     * @param element
     * @return
     */
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

    /**
     * Get the list of id's of the Concept's related concepts
     * 
     * @param element
     * @return
     */
    private List<String> getRelatedConceptIds(Element element) {
    	List<String> ids = new ArrayList<String>();
    	
    	NodeList nodeList = element.getElementsByTagName("skos:related");
        for(int i=0; i<nodeList.getLength(); i++){
        	  Node typeNode = nodeList.item(i);
        	  Node namedItem = typeNode.getAttributes().getNamedItem("rdf:resource");
        	  ids.add(namedItem.getNodeValue());
        }
        
    	return ids;
    }
   
    /**
     * Create the Vocabulary structure by creating all relations (BT/NT/RT) between the concepts
     * 
     * Note that we want this to be done in the same database 'transaction' 
     * and this 'current' one is not finished. 
     * Therefore the Concepts are not retrievable from the database yet 
     * and we need to keep them in out lookup!
     *  
     * @param doc
     * @param eventContext
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError
     */
    private void createVocabularyStruture(Document doc, final EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError, IntegrityError {
    	
	   	 logger.debug("Number of concepts in lookup: " + conceptLookup.size());
	   	 
	   	 createBroaderNarrowerRelations(doc, eventContext, manifest);
	   	 createNonspecificRelations(doc, eventContext, manifest);
    }
    
    /**
     * Create the broader/narrower relations for all the concepts
     * 
     * @param doc
     * @param eventContext
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError
     */
    private void createBroaderNarrowerRelations(Document doc, final EventContext eventContext,
    			final ImportLog manifest) throws ValidationError,
    			InvalidInputFormatError, IntegrityError {
    	
	   	 // check the lookup and start making the BT relations (and narrower implicit)
	   	 // visit all concepts an see if they have broader concepts
	     for (String skosId : conceptLookup.keySet()) {
	    	 ConceptPlaceholder conceptPlaceholder = conceptLookup.get(skosId);
	    	 if (!conceptPlaceholder.broaderIds.isEmpty()) {
	    		 logger.debug("Concept with skos id [" + skosId 
	    				 + "] has broader concepts");
	    		 // find them in the lookup
	    		 for (String bsId: conceptPlaceholder.broaderIds) {
	    			 if (conceptLookup.containsKey(bsId)) {
	    				 // found
	    				 logger.debug("Found mapping from: " + bsId 
	    						 + " to: " + conceptLookup.get(bsId).storeId);
	    				 
	    				 createBroaderNarrowerRelation(conceptLookup.get(bsId), conceptPlaceholder);
	    			 } else {
	    				 // not found
	    				 logger.debug("Found NO mapping for: " + bsId); 
	    				 // NOTE What does this mean; refers to an External resource, not in this file?
	    			 }
	    		 }
	    	 }
	     }    	
    }
    
    /**
     * Create the Broader to Narrower Concept relation
     * Note that we cannot use the storeId's and retrieve them from the database. 
     * we need to use the Concept objects from the placeholders in the lookup. 
     * 
     * @param bcp
     * @param ncp
     */
    private void createBroaderNarrowerRelation(ConceptPlaceholder bcp, ConceptPlaceholder ncp)  {
    	logger.debug("Creating Broader: " + bcp.storeId + " to Narrower: " + ncp.storeId);
    	bcp.concept.addNarrowerConcept(ncp.concept);
    }

    /**
     * Create the 'non-specific' relations for all the concepts
     * 
     * @param doc
     * @param eventContext
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError
     */
    private void createNonspecificRelations(Document doc, final EventContext eventContext,
    			final ImportLog manifest) throws ValidationError,
    			InvalidInputFormatError, IntegrityError {
    	
	   	 // check the lookup and start making the relations
	   	 // visit all concepts an see if they have a related concept    	
	     for (String skosId : conceptLookup.keySet()) {
	    	 ConceptPlaceholder conceptPlaceholder = conceptLookup.get(skosId);
	    	 if (!conceptPlaceholder.relatedIds.isEmpty()) {
	    		 logger.debug("Concept with skos id [" + skosId 
	    				 + "] has related concepts");
	    		 // find them in the lookup
	    		 for (String toCId: conceptPlaceholder.relatedIds) {
	    			 if (conceptLookup.containsKey(toCId)) {
	    				 // found
	    				 logger.debug("Found mapping from: " + toCId 
	    						 + " to: " + conceptLookup.get(toCId).storeId);
	    				 
	    				 createNonspecificRelation(conceptPlaceholder, conceptLookup.get(toCId));
	    			 } else {
	    				 // not found
	    				 logger.debug("Found NO mapping for: " + toCId); 
	    				 // NOTE What does this mean; refers to an External resource, not in this file?
	    			 }
	    		 }
	    	 }
	     }    	
    }
    
    /**
     * Create a non specific relation; this is the 'skos:related' on a concept 
     * @param from
     * @param to
     */
    private void createNonspecificRelation(ConceptPlaceholder from, ConceptPlaceholder to)  {
    	
    	// Prevent creating the relation both ways
    	// NOTE, optimization possible if we could prevent for checking everything twice!
    	if (!reverseRelationExists(from, to)) {
        	logger.debug("Creating relation from: " + from.storeId + " to: " + to.storeId);    	
    		from.concept.addRelatedConcept(to.concept);
    	}
    }
    
    /**
     * Check if the 'reverse' relation is exists already, via relatedBy 
     * 
     * @param from
     * @param to
     * @return
     */
    private boolean reverseRelationExists(ConceptPlaceholder from, ConceptPlaceholder to) {
    	boolean result = false;
    	for(Concept relatedBy: from.concept.getRelatedByConcepts()) {
    		//logger.debug("Related By: " + relatedBy.asVertex().getProperty(EntityType.ID_KEY));
    		String relatedByStoreId = (String) relatedBy.asVertex().getProperty(EntityType.ID_KEY);
    		if (relatedByStoreId == to.storeId) {
    			result = true;
    			break; // found
    		}
    	}
    	logger.debug("Relation exists: " + result + " for: " + to.storeId);
    	return result;
    }
    
    /**
     * Check if the element represents a Concept
     * 
     * @param element
     * @return
     */
    private boolean isConceptElement(Element element) {
    	boolean result = false;

    	// For now check if it has an rdf:type element 
    	// with the attribute rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"
    	
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
    
    /*** data extraction from XML below ***/
    
    /**
     * Extract the Concept information (but not the descriptions)
     * 
     * @param conceptNode
     * @return
     * @throws ValidationError
     */
    Map<String, Object> extractCvocConcept(Node conceptNode)
            throws ValidationError {
        Map<String, Object> dataMap = new HashMap<String, Object>();

        // are we using the rdf:about attribute as 'identifier'
        Node namedItem = conceptNode.getAttributes().getNamedItem("rdf:about");
        String value = namedItem.getNodeValue();
        dataMap.put(AccessibleEntity.IDENTIFIER_KEY, value);

        logger.debug("Extracting Concept id: " + dataMap.get(AccessibleEntity.IDENTIFIER_KEY));        
        
        return dataMap;
    }

    /**
     * Extract the Descriptions information for a concept
     * 
     * @param conceptElement
     * @return
     */
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

    /**
     * Extract a 'single valued' textual description property and add it to the data
     * 
     * @param descriptionData
     * @param textName
     * @param skosName
     * @param conceptElement
     */
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

    /**
     * Extract a 'multi valued' textual description property and add it to the data (list)
     * 
     * @param descriptionData
     * @param textName
     * @param skosName
     * @param conceptElement
     */
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

    /**
     * Create a description for a specific language or return the one created before
     * 
     * @param descriptionData
     * @param lang
     * @return
     */
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
     * Used in the lookup which is needed for creating the Vocabulary structure
     * 
     * @author paulboon
     *
     */
    private class ConceptPlaceholder {
    	public String storeId; // the identifier used for storage and referring in the repository
    	List<String> broaderIds;
    	List<String> relatedIds;
    	Concept concept;
    	
		public ConceptPlaceholder(String storeId,
				List<String> broaderIds,
				List<String> relatedIds,
				Concept concept) {
			this.storeId = storeId;
			this.broaderIds = broaderIds;
			this.relatedIds = relatedIds;
			
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
