package eu.ehri.project.exporters.ead;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.exporters.DocumentWriter;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.views.EventViews;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static eu.ehri.project.exporters.util.Helpers.createCDataElement;

/**
 * EAD 2002 Export.
 */
public class Ead2002Exporter implements EadExporter {
    private static final Logger logger = LoggerFactory.getLogger(Ead2002Exporter.class);
    protected static final DateTimeFormatter unitDateNormalFormat = DateTimeFormat.forPattern("YYYYMMdd");

    protected final FramedGraph<?> framedGraph;
    protected final EventViews eventManager;
    private final DocumentBuilder documentBuilder;

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
                    "postalcode",
                    "municipality",
                    "firstdem",
                    "countryCode",
                    "telephone",
                    "fax",
                    "webpage",
                    "email");

    public Ead2002Exporter(final FramedGraph<?> framedGraph) {
        this.framedGraph = framedGraph;
        eventManager = new EventViews(framedGraph);
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void export(DocumentaryUnit unit, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        new DocumentWriter(export(unit, langCode)).write(outputStream);
    }

    public Document export(DocumentaryUnit unit, String langCode) throws IOException {
        // Root
        Document doc = documentBuilder.newDocument();

        Element rootElem = doc.createElement("ead");
        rootElem.setAttribute("xmlns", "urn:isbn:1-931666-22-9");
        rootElem.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        rootElem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                "xs:schemaLocation", "urn:isbn:1-931666-22-9 http://www.loc.gov/ead/ead.xsd");
        doc.appendChild(rootElem);

        Element eadHeaderElem = doc.createElement("eadheader");
        eadHeaderElem.setAttribute("relatedencoding", "DC");
        eadHeaderElem.setAttribute("scriptencoding", "iso15924");
        eadHeaderElem.setAttribute("repositoryencoding", "iso15511");
        eadHeaderElem.setAttribute("dateencoding", "iso8601");
        eadHeaderElem.setAttribute("countryencoding", "iso3166-1");

        rootElem.appendChild(eadHeaderElem);
        Element eadIdElem = doc.createElement("eadid");
        eadIdElem.setTextContent(unit.getId());
        eadHeaderElem.appendChild(eadIdElem);

        Repository repository = unit.getRepository();
        Optional<Description> descOpt = eu.ehri.project.exporters.util.Helpers.getBestDescription(
                unit, Optional.<Description>absent(), langCode);

        for (Description desc : descOpt.asSet()) {
            addFileDesc(doc, eadHeaderElem, repository, desc, langCode);
            addProfileDesc(doc, eadHeaderElem, unit, desc);
        }

        addRevisionDesc(doc, eadHeaderElem, unit);

        //
        // Archdesc section
        //
        Element archDescElem = doc.createElement("archdesc");
        rootElem.appendChild(archDescElem);

        for (Description desc : descOpt.asSet()) {
            String level = Optional
                    .fromNullable(desc.<String>getProperty("levelOfDescription"))
                    .or("otherlevel");
            archDescElem.setAttribute("level", level);
            Element didElem = addDataSection(doc, archDescElem, unit, desc);

            for (Description repoDesc : eu.ehri.project.exporters.util.Helpers.getBestDescription(repository,
                    Optional.<Description>absent(), langCode).asSet()) {
                Element repoElem = doc.createElement("repository");
                didElem.appendChild(repoElem);
                Element corpElem = doc.createElement("corpname");
                repoElem.appendChild(corpElem);
                corpElem.setTextContent(repoDesc.getName());
            }

            addPropertyValues(doc, archDescElem, desc);
        }

        Element dscElem = doc.createElement("dsc");
        archDescElem.appendChild(dscElem);
        for (DocumentaryUnit child : getOrderedChildren(unit)) {
            addEadLevel(doc, dscElem, 1, child, descOpt, langCode);
        }

        for (Description desc : descOpt.asSet()) {
            addControlAccess(doc, archDescElem, desc);
        }

        return doc;
    }

    // Sort the children by identifier. FIXME: This might be a bad assumption!
    private Iterable<DocumentaryUnit> getOrderedChildren(DocumentaryUnit unit) {
        return Ordering.from(new Comparator<DocumentaryUnit>() {
            @Override
            public int compare(DocumentaryUnit c1, DocumentaryUnit c2) {
                return c1.getIdentifier().compareTo(c2.getIdentifier());
            }
        }).sortedCopy(unit.getChildren());
    }

    private void addRevisionDesc(Document doc, Element eadHeaderElem, DocumentaryUnit unit) {
        List<List<SystemEvent>> eventList = Lists.newArrayList(eventManager
                .aggregateForItem(unit, AnonymousAccessor.getInstance()));
        if (!eventList.isEmpty()) {
            Element revDescElem = doc.createElement("revisiondesc");
            eadHeaderElem.appendChild(revDescElem);

            for (int i = eventList.size() - 1; i >= 0; i--) {
                List<SystemEvent> agg = eventList.get(i);
                SystemEvent event = agg.get(0);
                Element changeElem = doc.createElement("change");
                revDescElem.appendChild(changeElem);
                Element dateElem = doc.createElement("date");
                changeElem.appendChild(dateElem);
                dateElem.setTextContent(new DateTime(event.getTimestamp()).toString());
                Element itemElem = doc.createElement("item");
                changeElem.appendChild(itemElem);
                if (event.getLogMessage() == null || event.getLogMessage().isEmpty()) {
                    itemElem.setTextContent(event.getEventType().name());
                } else {
                    itemElem.setTextContent(String.format("%s [%s]",
                            event.getLogMessage(), event.getEventType()));
                }
            }
        }
    }

    private void addFileDesc(Document doc, Element eadHeaderElem,
            Repository repository, Description desc, String langCode) {
        Element fileDescElem = doc.createElement("filedesc");
        eadHeaderElem.appendChild(fileDescElem);

        Element titleStmtElem = doc.createElement("titlestmt");
        Element properElem = doc.createElement("titleproper");
        properElem.setTextContent(desc.getName());
        titleStmtElem.appendChild(properElem);
        fileDescElem.appendChild(titleStmtElem);
        addPublicationStatement(doc, fileDescElem, repository, langCode);
    }

    private void addProfileDesc(Document doc, Element eadHeaderElem,
            DocumentaryUnit unit, Description desc) throws IOException {
        Element profDescElem = doc.createElement("profiledesc");
        eadHeaderElem.appendChild(profDescElem);

        Element creationElem = doc.createElement("creation");
        profDescElem.appendChild(creationElem);
        creationElem.setTextContent(
                Resources.toString(Resources.getResource("export-boilerplate.txt"),
                        StandardCharsets.UTF_8));
        Element creationDateElem = doc.createElement("date");
        creationElem.appendChild(creationDateElem);
        DateTime now = DateTime.now();
        creationDateElem.setAttribute("normal", unitDateNormalFormat.print(now));
        creationDateElem.setTextContent(now.toString());

        Element langUsageElem = doc.createElement("langusage");
        profDescElem.appendChild(langUsageElem);
        Element languageElem = doc.createElement("language");
        langUsageElem.appendChild(languageElem);
        languageElem.setAttribute("langcode", desc.getLanguageOfDescription());
        languageElem.setTextContent(Helpers.codeToName(desc.getLanguageOfDescription()));

        for (String value : Optional.fromNullable(
                desc.<String>getProperty("rulesAndConventions")).asSet()) {
            Element rulesElem = doc.createElement("descrules");
            profDescElem.appendChild(rulesElem);
            rulesElem.setAttribute("encodinganalog", "3.7.2");
            rulesElem.setTextContent(value);
        }
    }

    private void addPublicationStatement(Document doc, Element fileDescElem,
            Repository repository, String langCode) {
        Element pubStmtElem = doc.createElement("publicationstmt");
        fileDescElem.appendChild(pubStmtElem);

        for (Description repoDesc : eu.ehri.project.exporters.util.Helpers.getBestDescription(repository,
                Optional.<Description>absent(), langCode).asSet()) {
            Element publisherElem = doc.createElement("publisher");
            publisherElem.setTextContent(repoDesc.getName());
            pubStmtElem.appendChild(publisherElem);
            for (Address address : repoDesc.as(RepositoryDescription.class).getAddresses()) {
                Element addrElem = doc.createElement("address");
                pubStmtElem.appendChild(addrElem);
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
                Country country = repository.getCountry();
                Element lineElem = doc.createElement("addressline");
                addrElem.appendChild(lineElem);
                lineElem.setTextContent(new java.util.Locale(Locale.ENGLISH.getLanguage(), country.getCode())
                        .getDisplayCountry());
            }
        }
    }

    public void addEadLevel(Document doc, Element base, int num, DocumentaryUnit subUnit,
            Optional<Description> priorDescOpt, String langCode) throws IOException {
        logger.trace("Adding EAD sublevel: c" + num + " -> " + base.getTagName());
        Optional<Description> descOpt = eu.ehri.project.exporters.util.Helpers.getBestDescription(subUnit, priorDescOpt, langCode);
        String levelTag = String.format("c%02d", num);
        Element levelElem = doc.createElement(levelTag);
        base.appendChild(levelElem);

        for (Description desc : descOpt.asSet()) {
            for (String level : Optional.fromNullable(
                    desc.<String>getProperty("levelOfDescription")).asSet()) {
                levelElem.setAttribute("level", level);
            }
        }

        for (Description desc : descOpt.asSet()) {
            addDataSection(doc, levelElem, subUnit, desc);
            addPropertyValues(doc, levelElem, desc);
            addControlAccess(doc, levelElem, desc);
        }

        for (DocumentaryUnit child : getOrderedChildren(subUnit)) {
            addEadLevel(doc, levelElem, num + 1, child, descOpt, langCode);
        }
    }

    private Element addDataSection(Document doc, Element levelElem, DocumentaryUnit subUnit, Description desc)
            throws IOException {
        Element didElem = doc.createElement("did");
        levelElem.appendChild(didElem);
        Element unitIdElem = doc.createElement("unitid");
        didElem.appendChild(unitIdElem);
        unitIdElem.setTextContent(subUnit.getIdentifier());

        Set<String> propertyKeys = desc.getPropertyKeys();

        Element unitTitleElem = doc.createElement("unittitle");
        didElem.appendChild(unitTitleElem);
        unitTitleElem.setTextContent(desc.getName());

        for (DatePeriod datePeriod : desc.as(DocumentDescription.class).getDatePeriods()) {
            if (DatePeriod.DatePeriodType.creation.equals(datePeriod.getDateType())) {
                String start = datePeriod.getStartDate();
                String end = datePeriod.getEndDate();
                Element dateElem = doc.createElement("unitdate");
                if (start != null && end != null) {
                    DateTime startDateTime = new DateTime(start);
                    DateTime endDateTime = new DateTime(end);
                    dateElem.setAttribute("normal", String.format("%s/%s",
                            unitDateNormalFormat.print(startDateTime),
                            unitDateNormalFormat.print(endDateTime)));
                    dateElem.setTextContent(String.format("%s/%s",
                            startDateTime.year().get(), endDateTime.year().get()));
                    didElem.appendChild(dateElem);
                } else if (start != null) {
                    DateTime startDateTime = new DateTime(start);
                    dateElem.setAttribute("normal", String.format("%s",
                            unitDateNormalFormat.print(startDateTime)));
                    dateElem.setTextContent(String.format("%s", startDateTime.year().get()));
                    didElem.appendChild(dateElem);
                }
            }
        }

        for (Map.Entry<String, String> pair : textDidMappings.entrySet()) {
            if (propertyKeys.contains(pair.getKey())) {
                Object value = desc.getProperty(pair.getKey());
                List values = value instanceof List ? (List) value : Lists.newArrayList(value);
                for (Object v : values) {
                    Element elem = doc.createElement(pair.getValue());
                    didElem.appendChild(elem);
                    elem.setTextContent(v.toString());
                }
            }
        }

        if (propertyKeys.contains("languageOfMaterial")) {
            Element langMatElem = doc.createElement("langmaterial");
            didElem.appendChild(langMatElem);
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
                langMatElem.appendChild(lang);
            }
        }
        return didElem;
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
            Element ctrlElem = doc.createElement("controlaccess");
            element.appendChild(ctrlElem);
            for (AccessPoint accessPoint : entry.getValue()) {
                Element apElem = doc.createElement(controlAccessMappings.get(type));
                apElem.setTextContent(accessPoint.getName());
                ctrlElem.appendChild(apElem);
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
                    Element elem = doc.createElement(pair.getValue());
                    base.appendChild(elem);
                    createCDataElement(doc, elem, "p", v.toString());
                }
            }
        }
    }
}
