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
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Description;
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
 * Handler of Virtual EAD files.
 */
public class VirtualEadHandler extends SaxXmlHandler {
    public static final String AUTHOR = "authors",
            SOURCEFILEID = "sourceFileId";
    final List<MaintenanceEvent> maintenanceEvents = Lists.newArrayList();
    private final ImmutableMap<String, Class<? extends Entity>> possibleSubnodes = ImmutableMap.<String, Class<?
            extends Entity>>of("maintenanceEvent", MaintenanceEvent.class);

    private static final Logger logger = LoggerFactory
            .getLogger(VirtualEadHandler.class);

    protected final List<AbstractUnit>[] children = new ArrayList[12];
    protected final Stack<String> scopeIds = new Stack<>();
    // Pattern for EAD nodes that represent a child item
    private final static Pattern childItemPattern = Pattern.compile("^/*c(?:\\d*)$");

    // Constants for elements we need to watch for.
    private final static String ARCHDESC = "archdesc";
    private final static String DID = "did";
    private final static String DEFAULT_LANGUAGE = "eng";
    /**
     * used to attach the MaintenanceEvents to
     */
    private VirtualUnit topLevel;

    /**
     * Default language to use in units without language
     */
    protected String eadLanguage = DEFAULT_LANGUAGE;

    /**
     * EAD identifier as found in <code>&lt;eadid&gt;</code> in the currently handled EAD file
     */
    private String eadId;
    private String author;

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
     * @param importer
     */
    @SuppressWarnings("unchecked")
    public VirtualEadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties("vc.properties"));
        logger.warn("vc.properties used");
    }

    /**
     * Create an EadHandler using some importer, and a mapping of paths to node properties.
     *
     * @param importer
     * @param xmlImportProperties
     */
    public VirtualEadHandler(AbstractImporter<Map<String, Object>> importer,
            XmlImportProperties xmlImportProperties) {
        super(importer, xmlImportProperties);
        children[depth] = Lists.newArrayList();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (isUnitDelimiter(qName)) { //a new Unit should be created
            children[depth] = Lists.newArrayList();
        }
    }

    protected List<String> pathIds() {
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
//    	logger.debug("localName: " + localName + ", qName: " + qName);
        if (localName.equals("eadid") || qName.equals("eadid")) {
            eadId = (String) currentGraphPath.peek().get(SOURCEFILEID);
            logger.debug("Found <eadid>: " + eadId);
        } else if (localName.equals("author") || qName.equals("author")) {
            author = (String) currentGraphPath.peek().get(AUTHOR);
            logger.debug("Found <author>: " + author);
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
            logger.debug("Current id path: " + scopeIds);
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

                    currentGraph.put(SOURCEFILEID, getSourceFileId());

                    //add the <author> of the ead to every description
                    addAuthor(currentGraph);

                    AbstractUnit current = (AbstractUnit) importer.importItem(currentGraph, pathIds());
                    //add the maintenanceEvents, but only to the DD that was just created
                    for (Description dd : current.getDescriptions()) {
                        if (getSourceFileId().equals(dd.getProperty(SOURCEFILEID))) {
                            for (MaintenanceEvent me : maintenanceEvents) {
                                dd.addMaintenanceEvent(me);
                            }
                        }
                    }
                    if (current.getType().equals(Entities.VIRTUAL_UNIT)) {
                        logger.debug("virtual unit created: " + current.getIdentifier());
                        topLevel = (VirtualUnit) current; // if it is not overwritten, the current DU is the topLevel
                        logger.debug("importer used: " + importer.getClass());
                        if (depth > 0) { // if not on root level
                            children[depth - 1].add(current); // add child to parent offspring
                            // set the parent child relationships by hand
                            // as we don't have the parent Documentary unit yet.
                            // only when closing a DocUnit, one can set the relationship to its children,
                            // but not its parent, as that has not yet been closed.
                            for (AbstractUnit child : children[depth]) {
                                if (child != null) {
                                    if (child.getType().equals(Entities.VIRTUAL_UNIT)) {
                                        logger.debug("virtual child");

                                        ((VirtualUnit) current).addChild(((VirtualUnit) child));
                                        child.setPermissionScope(current);
                                    } else { //child.getType().equals(Entities.DOCUMENTARY_UNIT)
                                        logger.debug("documentary child");
                                        ((VirtualUnit) current).addIncludedUnit(((DocumentaryUnit) child));

                                    }
                                }
                            }
                        }
                    } else {
                        //nothing has to happen, since the DocumentaryUnit is already created before
                        logger.debug("documentary Unit found: " + current.getIdentifier());
                        if (depth > 0) { // if not on root level
                            children[depth - 1].add(current); // add child to parent offspring
                        }
                    }
                } catch (ValidationError ex) {
                    logger.error("caught validation error: " + ex.getMessage());
                } finally {
                    depth--;
                    scopeIds.pop();
                }
            } else {
                //import the MaintenanceEvent
                if (getImportantPath(currentPath).equals("maintenanceEvent")) {
                    Map<String, Object> me = importer.getMaintenanceEvent(currentGraph);
                    maintenanceEvents.add(importer.importMaintenanceEvent(me));
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

    protected String getSourceFileId() {
        if (getEadId()
                .toLowerCase()
                .endsWith("#" + getDefaultLanguage()
                        .toLowerCase()))
            return getEadId();
        return getEadId() + "#" + getDefaultLanguage().toUpperCase();
    }

    /**
     * Get the EAD identifier of the EAD being imported
     *
     * @return the <code>&lt;eadid&gt;</code> or null if it was not parsed yet or empty
     */
    protected String getEadId() {
        if (eadId == null)
            logger.error("eadid not set yet or empty");
        return eadId;
    }

    protected String getAuthor() {
        return author;
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph Data at the current node level
     */
    protected void useDefaultLanguage(Map<String, Object> currentGraph) {
        useDefaultLanguage(currentGraph, getDefaultLanguage());
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph    Data at the current node level
     * @param defaultLanguage Language code to use as default
     */
    protected void useDefaultLanguage(Map<String, Object> currentGraph, String defaultLanguage) {

        if (!currentGraph.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            logger.debug("Using default language code: " + defaultLanguage);
            currentGraph.put(Ontology.LANGUAGE_OF_DESCRIPTION, defaultLanguage);
        }
    }

    protected String getDefaultLanguage() {
        return eadLanguage;
    }

    /**
     * Handler-specific code for extraction or generation of unit titles.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractTitle(Map<String, Object> currentGraph) {

    }

    /**
     * Handler-specific code for extraction or generation of unit dates.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractDate(Map<String, Object> currentGraph) {

    }

    /**
     * Handler-specific code for extraction or generation of unit IDs.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractIdentifier(Map<String, Object> currentGraph) {

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
            logger.debug("adding alternative id: " + otherIdentifier);
            Object oids = currentGraph.get(Ontology.OTHER_IDENTIFIERS);
            if (oids != null && oids instanceof ArrayList<?>) {
                ((ArrayList<String>) oids).add(otherIdentifier);
                logger.debug("alternative ID added");
            }
        } else {
            logger.debug("adding first alt id: " + otherIdentifier);
            List<String> oids = Lists.newArrayList();
            oids.add(otherIdentifier);
            currentGraph.put(Ontology.OTHER_IDENTIFIERS, oids);
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
    protected static boolean isUnitDelimiter(String elementName) {
        return childItemPattern.matcher(elementName).matches() || elementName.equals(ARCHDESC);
    }

    private void addAuthor(Map<String, Object> currentGraph) {
        if (getAuthor() != null && !currentGraph.containsKey(AUTHOR)) {
            currentGraph.put(AUTHOR, getAuthor());
        }
    }
}
