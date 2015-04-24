/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.cvoc;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.ActionManager.EventContext;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.util.List;
import java.util.Map;

/**
 * Importer for the controlled vocabulary (TemaTres thesaurus)
 * specified by a SKOS-Core RDF file.
 * <p/>
 * This is different from the other importing mainly because I don't fully understand that.
 * So this importer will also do the 'management' and we will see later how to refactor it.
 * <p/>
 * Note: most code was copied from the EadImportManager and its base classes
 * Also note that: We don't have an Repository for the CVOCs, but a Vocabulary instead!
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 */
public final class XmlSkosImporter implements SkosImporter {
    private static final Logger logger = LoggerFactory
            .getLogger(XmlSkosImporter.class);

    private final FramedGraph<?> framedGraph;
    private final Actioner actioner;
    private final Boolean tolerant;
    private final Vocabulary vocabulary;
    private final String format;

    private static final String CONCEPT_URL = "url";

    // map from the internal Skos identifier to the placeholder
    private final Map<String, ConceptPlaceholder> conceptLookup = Maps.newHashMap();

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }

    /**
     * Constructor.
     *
     * @param framedGraph The framed graph
     * @param actioner    The actioner
     * @param vocabulary  The vocabulary
     * @param tolerant    Whether or not to allow single item validation errors
     * @param format      The RDF format
     */
    public XmlSkosImporter(FramedGraph<?> framedGraph,
            final Actioner actioner, Vocabulary vocabulary, boolean tolerant, String format) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
        this.tolerant = tolerant;
        this.format = format;
    }

    /**
     * Constructor.
     *
     * @param framedGraph The framed graph
     * @param actioner    The actioner
     * @param vocabulary  The vocabulary
     */
    public XmlSkosImporter(FramedGraph<?> framedGraph,
            final Actioner actioner, Vocabulary vocabulary) {
        this(framedGraph, actioner, vocabulary, false, null);
    }

    public XmlSkosImporter setTolerant(boolean tolerant) {
        logger.debug("Setting importer to tolerant: " + tolerant);
        return new XmlSkosImporter(framedGraph, actioner, vocabulary, tolerant, format);
    }

    public XmlSkosImporter setFormat(String format) {
        throw new UnsupportedOperationException("Only RDF/XML is supported");
    }

    public XmlSkosImporter setDefaultLang(String lang) {
        throw new UnsupportedOperationException(
                "Setting default lang is not currently supported");
    }

    /**
     * management part **
     */

    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError {
        try (FileInputStream ios = new FileInputStream(filePath)) {
            return importFile(ios, logMessage);
        }
    }

    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {
        try {
            // Create a new action for this import
            final EventContext eventContext = new ActionManager(framedGraph, vocabulary).newEventContext(
                    actioner, EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(eventContext);

            // Do the import...
            importFile(ios, eventContext, log);
            // If nothing was imported, remove the action...
            if (log.hasDoneWork()) {
                eventContext.commit();
            }

            return log;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidXmlDocument, InvalidInputFormatError {

        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(!tolerant);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (tolerant)
                builder.setEntityResolver(new DummyEntityResolver());
            Document doc = builder.parse(ios);
            logger.debug("xml encoding: " + doc.getXmlEncoding());
            importDocWithinAction(doc, eventContext, log);
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        } catch (SAXException e) {
            logger.error(e.getMessage());
            throw new InputParseError(e);
        }

    }

    /*** End of management part, Vocabulary/Concept code below ***/

    /**
     * Import an RDF/XML doc using the given action.
     *
     * @param doc
     * @param eventContext
     * @throws ValidationError
     * @throws InvalidInputFormatError
     */
    private void importDocWithinAction(Document doc, final ActionManager.EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError {

        createConcepts(doc, eventContext, manifest);
        createVocabularyStructure(doc, eventContext, manifest);
    }

    /**
     * Do the Concept data extraction and create all the Concepts
     *
     * @param doc
     * @param action
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     */
    private void createConcepts(Document doc, final ActionManager.EventContext action,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError {

        // the Concept is extracted from the XML
        // but how to use the namespacing properly?
        // for now suppose the root node is rdf:RDF
        if (doc.getDocumentElement().getNodeName().equals("rdf:RDF")) {
            Element rdfElement = doc.getDocumentElement();
            // get all concepts,  without XPath
            // NOTE the concept can be in a skos:Concept or an rdf:Description
            // for now assume we have RDF !
            NodeList nodeList = rdfElement.getElementsByTagName("rdf:Description");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = nodeList.item(i);
                Element element = (Element) childNode; // it should be!

                if (isConceptElement(element)) {
                    try {
                        Bundle unit = constructBundleForConcept(element);

                        BundleDAO persister = new BundleDAO(framedGraph, vocabulary.idPath());
                        Mutation<Concept> mutation = persister.createOrUpdate(unit,
                                Concept.class);
                        Concept frame = mutation.getNode();


                        // Set the vocabulary/concept relationship
                        handleCallbacks(mutation, manifest);

                        if (mutation.created() || mutation.unchanged()) {
                            // when concept was successfully persisted!
                            action.addSubjects(frame);
                        }

                        // FIXME: Handle case where relationships have changed on update???
                        if (mutation.created()) {
                            frame.setVocabulary(vocabulary);
                            frame.setPermissionScope(vocabulary);

                            // Create and add a ConceptPlaceholder
                            // for making the vocabulary (relation) structure in the next step
                            List<String> broaderIds = getBroaderConceptIds(element);
                            logger.debug("Concept has " + broaderIds.size()
                                    + " broader ids: " + broaderIds);
                            List<String> relatedIds = getRelatedConceptIds(element);
                            logger.debug("Concept has " + relatedIds.size()
                                    + " related ids: " + relatedIds);

                            String storeId = unit.getId();//id;
                            String skosId = unit.getDataValue(CONCEPT_URL);
                            // referal
                            logger.debug("Concept store id = " + storeId + ", skos id = " + skosId);
                            conceptLookup.put(skosId, new ConceptPlaceholder(storeId, broaderIds, relatedIds, frame));
                        }
                    } catch (ValidationError validationError) {
                        if (tolerant) {
                            logger.error(validationError.getMessage());
                        } else {
                            throw validationError;
                        }
                    }
                }
            }
        }
    }

    /**
     * Extract data and construct the bundle for a new Concept
     *
     * @param element
     * @throws ValidationError
     */
    private Bundle constructBundleForConcept(Element element) throws ValidationError {
        Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT, extractCvocConcept(element));

        // add the description data to the concept as relationship
        Map<String, Object> descriptions = extractCvocConceptDescriptions(element);

        for (String key : descriptions.keySet()) {
            logger.debug("description for: " + key);
            Map<String, Object> d = (Map<String, Object>) descriptions.get(key);
            logger.debug("languageCode = " + d.get(Ontology.LANGUAGE_OF_DESCRIPTION));

            Bundle descBundle = new Bundle(EntityClass.CVOC_CONCEPT_DESCRIPTION, d);
            Map<String, Object> rel = extractRelations(element, "owl:sameAs");
            if (!rel.isEmpty()) {
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT,
                        new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
            }

            // NOTE maybe test if prefLabel is there?
            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }

        return unit;
    }

    /**
     * Get the list of id's of the Concept's broader concepts
     *
     * @param element
     */
    private List<String> getBroaderConceptIds(Element element) {
        List<String> ids = Lists.newArrayList();
        NodeList nodeList = element.getElementsByTagName("skos:broader");
        for (int i = 0; i < nodeList.getLength(); i++) {
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
     */
    private List<String> getRelatedConceptIds(Element element) {
        List<String> ids = Lists.newArrayList();
        NodeList nodeList = element.getElementsByTagName("skos:related");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node typeNode = nodeList.item(i);
            Node namedItem = typeNode.getAttributes().getNamedItem("rdf:resource");
            ids.add(namedItem.getNodeValue());
        }

        return ids;
    }

    /**
     * Create the Vocabulary structure by creating all relations (BT/NT/RT) between the concepts
     * <p/>
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
     */
    private void createVocabularyStructure(Document doc, final EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError {

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
     */
    private void createBroaderNarrowerRelations(Document doc, final EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError {

        // check the lookup and start making the BT relations (and narrower implicit)
        // visit all concepts an see if they have broader concepts
        for (String skosId : conceptLookup.keySet()) {
            ConceptPlaceholder conceptPlaceholder = conceptLookup.get(skosId);
            if (!conceptPlaceholder.broaderIds.isEmpty()) {
                logger.debug("Concept with skos id [" + skosId
                        + "] has broader concepts of " + conceptPlaceholder.broaderIds.size());
                // find them in the lookup
                for (String bsId : conceptPlaceholder.broaderIds) {
                    logger.debug(bsId);
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
    private void createBroaderNarrowerRelation(ConceptPlaceholder bcp, ConceptPlaceholder ncp) {
        logger.debug("Creating Broader: " + bcp.storeId + " to Narrower: " + ncp.storeId);

        // An item CANNOT be a narrower version of itself!
        if (bcp.concept.equals(ncp.concept)) {
            logger.error("Ignoring cyclic narrower relationship on {}", bcp.concept.getId());
        } else {
            bcp.concept.addNarrowerConcept(ncp.concept);
        }
    }

    /**
     * Create the 'non-specific' relations for all the concepts
     *
     * @param doc
     * @param eventContext
     * @param manifest
     * @throws ValidationError
     * @throws InvalidInputFormatError
     */
    private void createNonspecificRelations(Document doc, final EventContext eventContext,
            final ImportLog manifest) throws ValidationError,
            InvalidInputFormatError {

        // check the lookup and start making the relations
        // visit all concepts an see if they have a related concept
        for (String skosId : conceptLookup.keySet()) {
            ConceptPlaceholder conceptPlaceholder = conceptLookup.get(skosId);
            if (!conceptPlaceholder.relatedIds.isEmpty()) {
                logger.debug("Concept with skos id [" + skosId
                        + "] has related concepts");
                // find them in the lookup
                for (String toCId : conceptPlaceholder.relatedIds) {
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
     *
     * @param from
     * @param to
     */
    private void createNonspecificRelation(ConceptPlaceholder from, ConceptPlaceholder to) {

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
     */
    private boolean reverseRelationExists(ConceptPlaceholder from, ConceptPlaceholder to) {
        boolean result = false;
        for (Concept relatedBy : from.concept.getRelatedByConcepts()) {
            //logger.debug("Related By: " + relatedBy.asVertex().getProperty(EntityType.ID_KEY));
            String relatedByStoreId = relatedBy.asVertex().getProperty(EntityType.ID_KEY);
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
     */
    private boolean isConceptElement(Element element) {
        boolean result = false;

        // For now check if it has an rdf:type element
        // with the attribute rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"

        NodeList nodeList = element.getElementsByTagName("rdf:type");
        for (int i = 0; i < nodeList.getLength(); i++) {
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
     * @throws ValidationError
     */
    Map<String, Object> extractCvocConcept(Node conceptNode)
            throws ValidationError {
        Map<String, Object> dataMap = Maps.newHashMap();

        // are we using the rdf:about attribute as 'identifier'
        Node namedItem = conceptNode.getAttributes().getNamedItem("rdf:about");
        String value = namedItem.getNodeValue();
        // Hack! Use everything after the last '/'
        String idvalue = value.substring(value.lastIndexOf('/') + 1);
        dataMap.put(Ontology.IDENTIFIER_KEY, idvalue);
        dataMap.put(CONCEPT_URL, value);

        logger.debug("Extracting Concept id: " + dataMap.get(Ontology.IDENTIFIER_KEY));

        return dataMap;
    }

    private Map<String, Object> extractRelations(Element element, String skosName) {
        Map<String, Object> relationNode = Maps.newHashMap();

        NodeList textNodeList = element.getElementsByTagName(skosName);
        for (int i = 0; i < textNodeList.getLength(); i++) {
            Node textNode = textNodeList.item(i);
            // get value
            String text = textNode.getTextContent();
            logger.debug("text: \"" + text + "\", skos name: " + skosName);

            // add to all descriptionData maps
            relationNode.put(Ontology.ANNOTATION_TYPE, skosName);
            relationNode.put(Ontology.NAME_KEY, text);
        }
        return relationNode;
    }

    /**
     * Extract the Descriptions information for a concept
     *
     * @param conceptElement
     */
    Map<String, Object> extractCvocConceptDescriptions(Element conceptElement) {
        // extract and process the textual items (with a language)
        // one description for each language, so the languageCode serve as a key into the map
        Map<String, Object> descriptionData = Maps.newHashMap();

        // one and only one
        extractAndAddToLanguageMapSingleValuedTextToDescriptionData(descriptionData, Ontology.NAME_KEY, "skos:prefLabel", conceptElement);
        // multiple alternatives is logical
        extractAndAddMultiValuedTextToDescriptionData(descriptionData,
                Ontology.CONCEPT_ALTLABEL, "skos:altLabel", conceptElement);
        // just allow multiple, its not forbidden by Skos
        extractAndAddMultiValuedTextToDescriptionData(descriptionData,
                Ontology.CONCEPT_SCOPENOTE, "skos:scopeNote", conceptElement);
        // just allow multiple, its not forbidden by Skos
        extractAndAddMultiValuedTextToDescriptionData(descriptionData,
                Ontology.CONCEPT_DEFINITION, "skos:definition", conceptElement);
        //<geo:lat>52.43333333333333</geo:lat>
        extractAndAddToAllMapsSingleValuedTextToDescriptionData(descriptionData, "latitude", "geo:lat", conceptElement);
        //<geo:long>20.716666666666665</geo:long>
        extractAndAddToAllMapsSingleValuedTextToDescriptionData(descriptionData, "longitude", "geo:long", conceptElement);

        //<owl:sameAs>http://www.yadvashem.org/yv/he/research/ghettos_encyclopedia/ghetto_details.asp?cid=1</owl:sameAs>
        //TODO: must be an UndeterminedRelation, which can then later be resolved

        // NOTE we could try to also add everything else, using the skos tagname as a key?

        return descriptionData;
    }

    private void extractAndAddToAllMapsSingleValuedTextToDescriptionData(Map<String, Object> descriptionData,
            String textName, String skosName, Element conceptElement) {
        NodeList textNodeList = conceptElement.getElementsByTagName(skosName);
        for (int i = 0; i < textNodeList.getLength(); i++) {
            Node textNode = textNodeList.item(i);
            // get value
            String text = textNode.getTextContent();
            logger.debug("text: \"" + text + "\", skos name: " + skosName + ", property: " + textName);

            // add to all descriptionData maps
            for (String key : descriptionData.keySet()) {
                Object map = descriptionData.get(key);
                if (map instanceof Map) {
                    ((Map<String, Object>) map).put(textName, text);
                } else {
                    logger.warn(key + " no description map found");
                }
            }
        }
    }

    /**
     * Extract a 'single valued' textual description property and add it to the data
     *
     * @param descriptionData
     * @param textName
     * @param skosName
     * @param conceptElement
     */
    private void extractAndAddToLanguageMapSingleValuedTextToDescriptionData(Map<String, Object> descriptionData,
            String textName, String skosName, Element conceptElement) {

        NodeList textNodeList = conceptElement.getElementsByTagName(skosName);
        for (int i = 0; i < textNodeList.getLength(); i++) {
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
        for (int i = 0; i < textNodeList.getLength(); i++) {
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
                ((List<String>) d.get(textName)).add(text);
            } else {
                // create a list first
                List<String> textList = Lists.newArrayList();
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
     */
    private Map<String, Object> getOrCreateDescriptionForLanguage(Map<String, Object> descriptionData, String lang) {
        Map<String, Object> d = null;
        if (descriptionData.containsKey(lang)) {
            d = (Map<String, Object>) descriptionData.get(lang);
        } else {
            // create one
            d = Maps.newHashMap();
            d.put("languageCode", lang); // initialize
            descriptionData.put(lang, d);
        }
        return d;
    }

    /*** ***/

    /**
     * Used in the lookup which is needed for creating the Vocabulary structure
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
     * tolerant mode, the EAD parser does not lookup the DTD and validateData the
     * document, which is both slow and error prone.
     */
    public class DummyEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicID, String systemID)
                throws SAXException {

            return new InputSource(new StringReader(""));
        }
    }

    protected void handleCallbacks(Mutation<? extends AccessibleEntity> mutation,
            ImportLog manifest) {
        switch (mutation.getState()) {
            case CREATED:
                manifest.addCreated();
                break;
            case UPDATED:
                manifest.addUpdated();
                break;
            case UNCHANGED:
                manifest.addUnchanged();
                break;
        }
    }
}
