package eu.ehri.project.exporters.ead;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.views.EventViews;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pegdown.LinkRenderer;
import org.pegdown.ParsingTimeoutException;
import org.pegdown.PegDownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EAD 2002 Export.
 * <p/>
 * NB: This class internally uses a non-thread-safe instance
 * of the Pegdown markdown renderer, and thus should not be
 * shared between threads.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Ead2002Exporter implements EadExporter {
    private static final Logger logger = LoggerFactory.getLogger(Ead2002Exporter.class);
    protected static final DateTimeFormatter unitDateNormalFormat = DateTimeFormat.forPattern("YYYYMMdd");

    protected final FramedGraph<?> framedGraph;
    protected final GraphManager manager;
    protected final EventViews eventManager;
    private final DocumentBuilder documentBuilder;
    protected final PegDownProcessor pegDownProcessor;

    public static final Map<String, String> multiValueTextMappings = ImmutableMap.<String, String>builder()
            .put("scopeAndContent", "scopecontent")
            .put("datesOfDescriptions", "processinfo")
            .put("systemOfArrangement", "arrangement")
            .put("publicationNote", "bibliography")
            .put("locationOfCopies", "altformavail")
            .put("locationOfOriginals", "originalsloc")
            .put("biographicalHistory", "bioghist")
            .put("conditionsOfAccess", "accessrestrict")
            .put("conditionsOfReproduction", "userestrict")
            .put("findingAids", "otherfindaid")
            .put("accruals", "accruals")
            .put("acquisition", "acqinfo")
            .put("appraisal", "appraisal")
            .put("archivalHistory", "custodhist")
            .put("physicalCharacteristics", "phystech")
            .put("notes", "odd") // controversial!
            .build();

    public static final Map<String, String> textDidMappings = ImmutableMap.<String, String>builder()
            .put("extentAndMedium", "physdesc")
            .put("abstract", "abstract")
            .put("unitDates", "unitdate")
            .build();

    public static final Map<String, String> controlAccessMappings = ImmutableMap.<String, String>builder()
            .put("subjectAccess", "subject")
            .put("personAccess", "persname")
            .put("familyAccess", "famname")
            .put("corporateBodyAccess", "corpname")
            .put("placeAccess", "geogname")
            .put("genreAccess", "genreform")
            .build();

    public static final List<String> addressKeys = ImmutableList
            .of("street",
                    "postalCode",
                    "municipality",
                    "firstdem",
                    "countryCode",
                    "telephone",
                    "fax",
                    "webpage",
                    "email");

    public Ead2002Exporter(final FramedGraph<?> framedGraph)
            throws IOException, ParserConfigurationException {
        this.framedGraph = framedGraph;
        manager = GraphManagerFactory.getInstance(framedGraph);
        eventManager = new EventViews(framedGraph);
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        pegDownProcessor = new PegDownProcessor(200); // millis parsing timeout
    }

    @Override
    public void export(DocumentaryUnit unit, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        new DocumentWriter(export(unit, langCode)).write(outputStream);
    }

    public Document export(DocumentaryUnit unit, String langCode) throws IOException {
        // Root
        Document doc = documentBuilder.newDocument();

        Element root = doc.createElement("ead");
        root.setAttribute("xmlns", "urn:isbn:1-931666-22-9");
        root.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        root.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                "xs:schemaLocation", "urn:isbn:1-931666-22-9 http://www.loc.gov/ead/ead.xsd");
        doc.appendChild(root);

        Element eadHeader = doc.createElement("eadheader");
        eadHeader.setAttribute("relatedencoding", "DC");
        eadHeader.setAttribute("scriptencoding", "iso15924");
        eadHeader.setAttribute("repositoryencoding", "iso15511");
        eadHeader.setAttribute("dateencoding", "iso8601");
        eadHeader.setAttribute("countryencoding", "iso3166-1");
        root.appendChild(eadHeader);
        Element eadId = doc.createElement("eadid");
        eadId.setTextContent(unit.getId());
        eadHeader.appendChild(eadId);

        Optional<Description> descOpt = getBestDescription(
                unit, Optional.<Description>absent(), langCode);
        Repository repository = unit.getRepository();

        eadHeader.appendChild(addFileDesc(doc, repository, descOpt, langCode));
        eadHeader.appendChild(addProfileDesc(doc, unit, descOpt));
        for (Element revDesc : addRevisionDesc(doc, unit).asSet()) {
            eadHeader.appendChild(revDesc);
        }

        //
        // Archdesc section
        //
        Element archDesc = doc.createElement("archdesc");
        for (Description desc : descOpt.asSet()) {
            String level = Optional
                    .fromNullable(desc.<String>getProperty("levelOfDescription"))
                    .or("otherlevel");
            archDesc.setAttribute("level", level);
        }
        root.appendChild(archDesc);

        Element did = addDataSection(doc, unit, descOpt, archDesc);
        for (Description repoDesc : getBestDescription(repository,
                Optional.<Description>absent(), langCode).asSet()) {
            Element repo = doc.createElement("repository");
            did.appendChild(repo);
            Element corp = doc.createElement("corpname");
            repo.appendChild(corp);
            corp.setTextContent(repoDesc.getName());
        }

        for (Description desc : descOpt.asSet()) {
            addPropertyValues(doc, archDesc, desc);
        }

        Element dsc = doc.createElement("dsc");
        archDesc.appendChild(dsc);
        for (DocumentaryUnit child : unit.getChildren()) {
            addEadLevel(doc, dsc, 1, child, descOpt, langCode);
        }

        for (Description desc : descOpt.asSet()) {
            addControlAccess(doc, archDesc, desc);
        }

        doc.setXmlStandalone(true);
        return doc;
    }

    private Optional<Element> addRevisionDesc(Document doc, DocumentaryUnit unit) {
        List<List<SystemEvent>> eventList = Lists.newArrayList(eventManager
                .aggregateForItem(unit, AnonymousAccessor.getInstance()));
        if (!eventList.isEmpty()) {
            Element revDesc = doc.createElement("revisiondesc");
            for (int i = eventList.size() - 1; i >= 0; i--) {
                List<SystemEvent> agg = eventList.get(i);
                SystemEvent event = agg.get(0);
                Element change = doc.createElement("change");
                revDesc.appendChild(change);
                Element date = doc.createElement("date");
                change.appendChild(date);
                date.setTextContent(new DateTime(event.getTimestamp()).toString());
                Element item = doc.createElement("item");
                change.appendChild(item);
                if (event.getLogMessage() == null || event.getLogMessage().isEmpty()) {
                    item.setTextContent(event.getEventType().name());
                } else {
                    item.setTextContent(String.format("%s [%s]",
                            event.getLogMessage(), event.getEventType()));
                }
            }
            return Optional.of(revDesc);
        }
        return Optional.absent();
    }

    private Element addFileDesc(Document doc, Repository repository,
            Optional<Description> descOpt, String langCode) {
        Element fileDesc = doc.createElement("filedesc");
        Element titleStmt = doc.createElement("titlestmt");

        Element proper = doc.createElement("titleproper");
        proper.setTextContent(descOpt.isPresent() ? descOpt.get().getName() : "UNTITLED");
        titleStmt.appendChild(proper);
        fileDesc.appendChild(titleStmt);
        fileDesc.appendChild(addPublicationStatement(doc, repository, langCode));

        return fileDesc;
    }

    private Element addProfileDesc(Document doc, DocumentaryUnit unit, Optional<Description> descOpt) {
        Element profDesc = doc.createElement("profiledesc");
        Element creation = doc.createElement("creation");
        profDesc.appendChild(creation);
        creation.setTextContent("This EAD file was exported from the EHRI admin database tool.");
        Element creationDate = doc.createElement("date");
        creation.appendChild(creationDate);
        DateTime now = DateTime.now();
        creationDate.setAttribute("normal", unitDateNormalFormat.print(now));
        creationDate.setTextContent(now.toString());

        for (Description desc : descOpt.asSet()) {
            Element langUsage = doc.createElement("langusage");
            profDesc.appendChild(langUsage);
            Element language = doc.createElement("language");
            langUsage.appendChild(language);
            language.setAttribute("langcode", desc.getLanguageOfDescription());
            language.setTextContent(Helpers.codeToName(desc.getLanguageOfDescription()));

            for (String value : Optional.fromNullable(
                    desc.<String>getProperty("rulesAndConventions")).asSet()) {
                Element rules = doc.createElement("descrules");
                profDesc.appendChild(rules);
                rules.setAttribute("encodinganalog", "3.7.2");
                rules.setTextContent(value);
            }
        }

        return profDesc;
    }

    private Element addPublicationStatement(Document doc, Repository repository, String langCode) {
        Element pubStmt = doc.createElement("publicationstmt");
        for (Description repoDesc : getBestDescription(repository,
                Optional.<Description>absent(), langCode).asSet()) {
            Element publisher = doc.createElement("publisher");
            publisher.setTextContent(repoDesc.getName());
            pubStmt.appendChild(publisher);
            for (Address address : manager.cast(repoDesc, RepositoryDescription.class).getAddresses()) {
                Element addrElem = doc.createElement("address");
                pubStmt.appendChild(addrElem);
                for (String key : addressKeys) {
                    Object value = address.getProperty(key);
                    if (value != null) {
                        List values = value instanceof List
                                ? (List) value : Lists.newArrayList(value);
                        for (Object v : values) {
                            Element line = doc.createElement("addressline");
                            addrElem.appendChild(line);
                            line.setTextContent(v.toString());
                        }
                    }
                }
            }
        }
        return pubStmt;
    }

    public Element addEadLevel(Document doc, Element base, int num, DocumentaryUnit subUnit,
            Optional<Description> priorDescOpt, String langCode) throws IOException {
        logger.trace("Adding EAD sublevel: c" + num + " -> " + base.getTagName());
        Optional<Description> descOpt = getBestDescription(subUnit, priorDescOpt, langCode);
        String levelTag = String.format("c%02d", num);
        Element levelElem = doc.createElement(levelTag);
        base.appendChild(levelElem);

        for (Description desc : descOpt.asSet()) {
            for (String level : Optional.fromNullable(
                    desc.<String>getProperty("levelOfDescription")).asSet()) {
                levelElem.setAttribute("level", level);
            }
        }

        addDataSection(doc, subUnit, descOpt, levelElem);

        for (Description desc : descOpt.asSet()) {
            addPropertyValues(doc, levelElem, desc);
        }

        for (Description desc : descOpt.asSet()) {
            addControlAccess(doc, levelElem, desc);
        }

        for (DocumentaryUnit child : subUnit.getChildren()) {
            addEadLevel(doc, levelElem, num + 1, child, descOpt, langCode);
        }

        return levelElem;
    }

    private Element addDataSection(Document doc, DocumentaryUnit subUnit, Optional<Description> descOpt,
            Element levelElem) throws IOException {
        Element did = doc.createElement("did");
        levelElem.appendChild(did);
        Element unitId = doc.createElement("unitid");
        did.appendChild(unitId);
        unitId.setTextContent(subUnit.getIdentifier());

        for (Description desc : descOpt.asSet()) {
            Set<String> propertyKeys = desc.getPropertyKeys();

            Element unitTitle = doc.createElement("unittitle");
            did.appendChild(unitTitle);
            unitTitle.setTextContent(desc.getName());

            for (DatePeriod datePeriod : manager.cast(desc, DocumentDescription.class).getDatePeriods()) {
                if (DatePeriod.DatePeriodType.creation.equals(datePeriod.getDateType())) {
                    String start = datePeriod.getStartDate();
                    String end = datePeriod.getEndDate();
                    Element date = doc.createElement("unitdate");
                    if (start != null && end != null) {
                        DateTime startDateTime = new DateTime(start);
                        DateTime endDateTime = new DateTime(end);
                        date.setAttribute("normal", String.format("%s/%s",
                                unitDateNormalFormat.print(startDateTime),
                                unitDateNormalFormat.print(endDateTime)));
                        date.setTextContent(String.format("%s/%s",
                                startDateTime.year().get(), endDateTime.year().get()));
                        did.appendChild(date);
                    } else if (start != null) {
                        DateTime startDateTime = new DateTime(start);
                        date.setAttribute("normal", String.format("%s",
                                unitDateNormalFormat.print(startDateTime)));
                        date.setTextContent(String.format("%s", startDateTime.year().get()));
                        did.appendChild(date);
                    }
                }
            }

            for (Map.Entry<String, String> pair : textDidMappings.entrySet()) {
                if (propertyKeys.contains(pair.getKey())) {
                    Object value = desc.getProperty(pair.getKey());
                    List values = value instanceof List ? (List) value : Lists.newArrayList(value);
                    for (Object v : values) {
                        Element ele = doc.createElement(pair.getValue());
                        did.appendChild(ele);
                        ele.setTextContent(v.toString());
                    }
                }
            }

            if (propertyKeys.contains("languageOfMaterial")) {
                Element langMat = doc.createElement("langmaterial");
                did.appendChild(langMat);
                Object value = desc.getProperty("languageOfMaterial");
                List values = value instanceof List
                        ? (List) value : Lists.newArrayList(value);
                for (Object v : values) {
                    Element lang = doc.createElement("language");
                    lang.setTextContent(Helpers.codeToName(v.toString()));
                    // Only add the language if its a 3-letter string
                    if (v.toString().length() == 3) {
                        lang.setAttribute("langcode", v.toString());
                    }
                    langMat.appendChild(lang);
                }
            }
        }
        return did;
    }

    private void addControlAccess(Document doc, Element element, Description desc) {
        Map<String, List<AccessPoint>> byType = Maps.newHashMap();
        for (AccessPoint accessPoint : desc.getAccessPoints()) {
            String type = accessPoint.getRelationshipType();
            if (controlAccessMappings.containsKey(type)) {
                if (byType.containsKey(type)) {
                    byType.get(type).add(accessPoint);
                } else {
                    byType.put(type, Lists.newArrayList(accessPoint));
                }
            }
        }

        for (Map.Entry<String, List<AccessPoint>> entry : byType.entrySet()) {
            String type = entry.getKey();
            Element ctrl = doc.createElement("controlaccess");
            element.appendChild(ctrl);
            for (AccessPoint accessPoint : entry.getValue()) {
                Element ap = doc.createElement(controlAccessMappings.get(type));
                ap.setTextContent(accessPoint.getName());
                ctrl.appendChild(ap);
            }
        }
    }

    private void addPropertyValues(Document doc, Element base, Frame item) throws IOException {
        Set<String> available = item.getPropertyKeys();

        for (Map.Entry<String, String> pair : multiValueTextMappings.entrySet()) {
            if (available.contains(pair.getKey())) {
                Object value = item.getProperty(pair.getKey());
                List values = value instanceof List
                        ? (List) value
                        : Lists.newArrayList(value);
                for (Object v : values) {
                    Element element = doc.createElement(pair.getValue());
                    base.appendChild(element);
                    appendTextValue(doc, element, item, v);
                }
            }
        }
    }

    private void appendTextValue(Document doc, Element element, Frame item, Object value) throws IOException {
        String textContent = new ToEadSerializer(new LinkRenderer())
                .toEad(pegDownProcessor.parseMarkdown(value.toString().toCharArray()));
        logger.trace("Text block {}: {}", element.getTagName(), textContent);
        try {
            Element text = documentBuilder
                    .parse(new ByteArrayInputStream(("<r>" + textContent + "</r>").getBytes("UTF-8")))
                    .getDocumentElement();
            NodeList childNodes = text.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                element.appendChild(doc.importNode(childNodes.item(i), true));
            }
        } catch (SAXException e) {
            logger.warn("EAD generation error for {} -> {}: error parsing input text: {}",
                    item.getId(), element.getTagName(), value);
            // If we can't do the data properly, stick it in CDATA
            addCData(doc, element, value);
        } catch (ParsingTimeoutException e) {
            logger.warn("EAD generation error for {} -> {}: Pegdown parser timeout at {} millis: {}",
                    item.getId(), element.getTagName(), value);
            addCData(doc, element, value);
        } catch (UnsupportedEncodingException e) {
            logger.warn("EAD generation error for {} -> {}: unsupported encoding for text: {}",
                    item.getId(), element.getTagName(), value);
            addCData(doc, element, value);
        }
    }

    private void addCData(Document doc, Element element, Object value) {
        Element p = doc.createElement("p");
        element.appendChild(p);
        CDATASection cdataSection = doc.createCDATASection(value.toString());
        p.appendChild(cdataSection);
    }

    private Optional<Description> getBestDescription(DescribedEntity item, Optional<Description> priorDescOpt, String langCode) {
        List<Description> descriptions = Lists.newArrayList(item.getDescriptions());
        Collections.sort(descriptions, new Comparator<Description>() {
            @Override
            public int compare(Description d1, Description d2) {
                return d1.getId().compareTo(d2.getId());
            }
        });
        Description fallBack = null;
        for (Description description : descriptions) {
            if (fallBack == null) {
                fallBack = description;
            }
            // First of all, check the description code (usually set to the
            // EAD file ID.) If this is the same as the parent, return the
            // current description.
            for (Description parent : priorDescOpt.asSet()) {
                for (String code : Optional.fromNullable(parent.getDescriptionCode()).asSet()) {
                    if (code.equals(description.getDescriptionCode())) {
                        return Optional.of(description);
                    }
                }
            }

            // Otherwise, fall back to the first one with the same language
            if (description.getLanguageOfDescription().equalsIgnoreCase(langCode)) {
                return Optional.of(description);
            }
        }
        return Optional.fromNullable(fallBack);
    }
}
