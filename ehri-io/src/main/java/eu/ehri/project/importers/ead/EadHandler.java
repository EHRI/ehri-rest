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

package eu.ehri.project.importers.ead;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Handler of EAD files. Use to create a representation of the structure of Documentary Units.
 * This generic handler does not do tricks to get data from any CHI-custom use of EAD - you
 * should extend this class for that.
 * If there is no language, it does set the language of the description to English.
 * makes use of icaatom.properties with format: part/of/path/=attribute
 */
public class EadHandler extends SaxXmlHandler {

    // Constants for elements we need to watch for.
    private static final String EADID = "eadid",
            DESCRULES = "descrules",
            MAINAGENCYCODE = "mainagencycode",
            AUTHOR = "author",
            RULES = "rulesAndConventions",
            ARCHDESC = "archdesc",
            DID = "did";

    private static final String DEFAULT_PROPERTIES = "ead2002.properties";

    private final List<Map<String, Object>> globalMaintenanceEvents = Lists.newArrayList();

    private final ImmutableMap<String, Class<? extends Entity>> possibleSubnodes =
            ImmutableMap.<String, Class<? extends Entity>>builder()
                    .put("maintenanceEvent", MaintenanceEvent.class).build();

    private static final Logger logger = LoggerFactory.getLogger(EadHandler.class);

    @SuppressWarnings("unchecked")
    protected final List<DocumentaryUnit>[] children = new ArrayList[12];

    /**
     * Stack of identifiers of archival units. Push/pop the identifier of the current
     * node on top/from the top of the stack.
     */
    private final Stack<String> scopeIds = new Stack<>();

    // Pattern for EAD nodes that represent a child item
    private final static Pattern childItemPattern = Pattern.compile("^/*c(?:\\d*)$");

    private final static String DEFAULT_LANGUAGE = "eng";

    /**
     * Default language to use in units without language
     */
    private String eadLanguage = DEFAULT_LANGUAGE;

    /**
     * EAD identifier as found in <code>&lt;eadid&gt;</code> in the currently handled EAD file
     */
    private final Map<String, String> eadfileValues;

    /**
     * Set a custom resolver so EAD DTDs are never looked up online.
     */
    @Override
    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
            throws org.xml.sax.SAXException, java.io.IOException {
        // This is the equivalent of returning a null dtd.
        return new org.xml.sax.InputSource(new java.io.StringReader(""));
    }

    /**
     * Create an EadHandler using some importer. The default mapping of paths to node properties is used.
     *
     * @param importer the importer instance
     */
    public EadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties(DEFAULT_PROPERTIES));
        logger.warn("Using default properties file: {}", DEFAULT_PROPERTIES);
    }

    /**
     * Create an EadHandler using some importer, and a mapping of paths to node properties.
     *
     * @param importer   the importer instance
     * @param properties an XML node properties instance
     */
    public EadHandler(AbstractImporter<Map<String, Object>> importer,
            XmlImportProperties properties) {
        super(importer, properties);
        children[depth] = Lists.newArrayList();
        eadfileValues = Maps.newHashMap();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (isUnitDelimiter(qName)) { //a new DocumentaryUnit should be created
            children[depth] = Lists.newArrayList();
        }
        if (qName.equals("profiledesc")) {
            putPropertyInCurrentGraph(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType.created.toString());
        }
        if (qName.equals("change")) {
            putPropertyInCurrentGraph(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType.updated.toString());
        }
        if (attributes.getValue(MAINAGENCYCODE) != null) {
            eadfileValues.put(MAINAGENCYCODE, attributes.getValue(MAINAGENCYCODE));
            logger.debug("Found @MAINAGENCYCODE: {}", eadfileValues.get(MAINAGENCYCODE));
        }

    }

    /**
     * Get the full 'path of identifiers' of the current node.
     *
     * @return a List of Strings, i.e. identifiers, representing the path of the current node
     */
    private List<String> pathIds() {
        if (scopeIds.isEmpty()) {
            return scopeIds;
        } else {
            List<String> path = Lists.newArrayList();
            for (int i = 0; i < scopeIds.size() - 1; i++) {
                path.add(scopeIds.get(i));
            }
            return path;
        }

    }

    private String getCurrentTopIdentifier() {
        Object current = currentGraphPath.peek().get(OBJECT_IDENTIFIER);
        if (current instanceof List<?>) {
            return (String) ((List) current).get(0);
        } else {
            return (String) current;
        }
    }

    /**
     * Called when the XML parser encounters an end tag. This is tuned for EAD files, which come in many flavours.
     * <p>
     * Certain elements represent subcollections, for which we create new nodes (here, we create representative Maps for nodes).
     * Many EAD producers use the standard in their own special way, so this method calls generalised methods to filter, get data
     * in the right place and reformat.
     * If a collection of EAD files need special treatment to get specific data in the right place, you only need to override the
     * other methods (in order: extractIdentifier, extractTitle, extractDate).
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);

        // If this is the <eadid> element, store its content
        switch (qName) {
            case EADID:
                eadfileValues.put(EADID, (String) currentGraphPath.peek().get(Ontology.SOURCEFILE_KEY));
                logger.debug("Found <{}>: {}", EADID, eadfileValues.get(EADID));
                break;
            case AUTHOR:
                eadfileValues.put(AUTHOR, (String) currentGraphPath.peek().get(AUTHOR));
                logger.debug("Found <{}>: {}", AUTHOR, eadfileValues.get(AUTHOR));
                break;
            case DESCRULES:
                eadfileValues.put(RULES, (String) currentGraphPath.peek().get(RULES));
                logger.debug("Found <{}>: {}", RULES, eadfileValues.get(RULES));
                break;
        }

        if (localName.equals("language") || qName.equals("language")) {
            String lang = (String) currentGraphPath.peek().get("languageCode");
            if (lang != null)
                eadLanguage = lang;
        }

        // FIXME: We need to add the 'parent' identifier to the ID stack
        // so that graph path IDs are created correctly. This currently
        // assumes there's a 'did' element from which we extract this
        // identifier.
        if (qName.equals(DID)) {
            extractIdentifier(currentGraphPath.peek());
            String topId = getCurrentTopIdentifier();
            scopeIds.push(topId);
            logger.debug("Current id path: {}", scopeIds);
        }

        if (needToCreateSubNode(qName)) {
            Map<String, Object> currentGraph = currentGraphPath.pop();

            if (isUnitDelimiter(qName)) {
                try {
                    //add any mandatory fields not yet there:
                    // First: identifier(s),
                    extractIdentifier(currentGraph);

                    // Second: title
                    extractTitle(currentGraph);

                    useDefaultLanguage(currentGraph);

                    extractDate(currentGraph);

                    //add eadid as sourceFileId
                    currentGraph.put(Ontology.SOURCEFILE_KEY, getSourceFileId());

                    //only on toplevel description:
                    if (qName.equals("archdesc")) {
                        //add the <author> of the ead to the processInfo
                        addAuthor(currentGraph);
                        addRules(currentGraph);
                    }

                    if (!globalMaintenanceEvents.isEmpty() && !currentGraph.containsKey("maintenanceEvent")) {
                        logger.debug("Adding global maintenance events: {}", globalMaintenanceEvents);
                        currentGraph.put("maintenanceEvent", globalMaintenanceEvents);
                    }

                    DocumentaryUnit current = (DocumentaryUnit) importer.importItem(currentGraph, pathIds());

                    /*
      used to attach the MaintenanceEvents to
     */
                    logger.debug("importer used: {}", importer.getClass());
                    if (depth > 0) { // if not on root level
                        children[depth - 1].add(current); // add child to parent offspring
                        // set the parent child relationships by hand
                        // as we don't have the parent Documentary unit yet.
                        // only when closing a DocUnit, one can set the relationship to its children,
                        // but not its parent, as that has not yet been closed.
                        for (DocumentaryUnit child : children[depth]) {
                            if (child != null) {
                                current.addChild(child);
                                child.setPermissionScope(current);
                            }
                        }
                    }
                } catch (ValidationError ex) {
                    logger.error("caught validation error", ex);
                } finally {
                    depth--;
                    scopeIds.pop();
                }
            } else {
                // import the MaintenanceEvent
                if (getImportantPath(currentPath).equals("maintenanceEvent")
                        && (qName.equals("profiledesc") || qName.equals("change"))) {
                    Map<String, Object> me = importer.getMaintenanceEvent(currentGraph);
                    me.put("order", globalMaintenanceEvents.size());
                    globalMaintenanceEvents.add(me);
                }
                putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
                depth--;
            }
        }

        currentPath.pop();
        if (currentPath.isEmpty()) {
            currentGraphPath.pop();
        }

    }

    /**
     * @return the <code>&lt;eadid&gt;</code>, extended with the languageTag or null if it was not parsed yet or empty
     */
    protected String getSourceFileId() {
        if (!eadfileValues.containsKey(EADID)) {
            logger.error("EADID not set yet, or not given in eadfile");
            return null;
        } else {
            String id = eadfileValues.get(EADID);
            String suffix = "#" + eadLanguage.toUpperCase();
            if (id.toUpperCase().endsWith(suffix)) {
                return id;
            }
            return id + suffix;
        }
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph Data at the current node level
     */
    private void useDefaultLanguage(Map<String, Object> currentGraph) {
        useDefaultLanguage(currentGraph, eadLanguage);
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph    Data at the current node level
     * @param defaultLanguage Language code to use as default
     */
    private void useDefaultLanguage(Map<String, Object> currentGraph, String defaultLanguage) {
        if (!currentGraph.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            logger.debug("Using default language code: {}", defaultLanguage);
            currentGraph.put(Ontology.LANGUAGE_OF_DESCRIPTION, defaultLanguage);
        }
    }

    /**
     * if no NAME_KEY is provided, use the IDENTIFIER_KEY
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractTitle(Map<String, Object> currentGraph) {
        if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
            logger.error("no name found, using identifier {}", currentGraph.get(OBJECT_IDENTIFIER));
            currentGraph.put(Ontology.NAME_KEY, currentGraph.get(OBJECT_IDENTIFIER));
        }
    }

    /**
     * Handler-specific code for extraction or generation of unit dates.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    @SuppressWarnings("unused")
    private void extractDate(Map<String, Object> currentGraph) {
    }

    /**
     * Handler-specific code for extraction or generation of unit IDs.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractIdentifier(Map<String, Object> currentGraph) {
        // If there are multiple identifiers at this point, take the
        // first and add the rest as alternate identifiers...
        if (currentGraph.containsKey(OBJECT_IDENTIFIER)) {
            Object idents = currentGraph.get(OBJECT_IDENTIFIER);
            if (idents instanceof List) {
                List identList = (List) idents;
                currentGraph.put(OBJECT_IDENTIFIER, identList.get(0));
                for (Object item : identList.subList(1, identList.size())) {
                    addOtherIdentifier(currentGraph, ((String) item));
                }
            }
        }
    }

    /**
     * Helper method to add identifiers to the list of other identifiers.
     * The property named Ontology.OTHER_IDENTIFIERS (i.e. "otherIdentifiers")
     * is always an ArrayList of Strings.
     *
     * @param currentGraph    the node representation to add the otherIdentifier to
     * @param otherIdentifier the alternative identifier to add
     */
    protected void addOtherIdentifier(Map<String, Object> currentGraph, String otherIdentifier) {
        if (currentGraph.containsKey(Ontology.OTHER_IDENTIFIERS)) {
            logger.debug("adding alternative id: {}", otherIdentifier);
            Object oids = currentGraph.get(Ontology.OTHER_IDENTIFIERS);
            if (oids instanceof List) {
                ((List<String>) oids).add(otherIdentifier);
            } else {
                currentGraph.put(Ontology.OTHER_IDENTIFIERS,
                        Lists.newArrayList(oids, otherIdentifier));
            }
        } else {
            logger.debug("adding first alt id: {}", otherIdentifier);
            currentGraph.put(Ontology.OTHER_IDENTIFIERS, Lists.newArrayList(otherIdentifier));
        }
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        //child or parent unit:
        boolean need = isUnitDelimiter(qName);
        //controlAccess 
        String path = getImportantPath(currentPath);
        if (path != null) {
            need = need || path.endsWith("AccessPoint");
        }
        return need || possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    /**
     * Determine if the element represents a unit delimiter
     *
     * @param elementName The XML element name
     * @return Whether or not we're moved to a new item
     */
    private static boolean isUnitDelimiter(String elementName) {
        return childItemPattern.matcher(elementName).matches() || elementName.equals(ARCHDESC);
    }

    private void addAuthor(Map<String, Object> currentGraph) {
        if (eadfileValues.containsKey(AUTHOR)) {
            Helpers.putPropertyInGraph(currentGraph, "processInfo", eadfileValues.get(AUTHOR));
        }
    }

    private void addRules(Map<String, Object> currentGraph) {
        if (eadfileValues.containsKey(RULES)) {
            Helpers.putPropertyInGraph(currentGraph, RULES, eadfileValues.get(RULES));
        }
    }
}
