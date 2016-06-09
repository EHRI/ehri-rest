package eu.ehri.project.exporters.eag;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.exporters.DocumentWriter;
import eu.ehri.project.utils.LanguageHelpers;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventAgentType;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.impl.EventsApiImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static eu.ehri.project.utils.LanguageHelpers.createCDataElement;

/**
 * Export EAG 2012 XML.
 */
public class Eag2012Exporter implements EagExporter {
    private static final Logger logger = LoggerFactory.getLogger(Eag2012Exporter.class);

    protected final FramedGraph<?> framedGraph;
    protected final EventsApi eventManager;
    private final DocumentBuilder documentBuilder;

    public static final Map<String, String> descriptiveTextMappings = ImmutableMap.<String, String>builder()
            .put("history", "repositorhist")
            .put("geoculturalContext", "repositorhist")
            .put("mandates", "repositorhist")
            .put("buildings", "buildinginfo/building")
            .put("holdings", "holdings")
            .put("conditions", "termsOfUse") // added to mandatory access
            .put("researchServices", "services/searchroom/researchServices")
            .build();

    public Eag2012Exporter(final FramedGraph<?> framedGraph) {
        this.framedGraph = framedGraph;
        eventManager = new EventsApiImpl(framedGraph, AnonymousAccessor.getInstance());
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void export(Repository repository, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        new DocumentWriter(export(repository, langCode)).write(outputStream);
    }

    @Override
    public Document export(Repository repository, String langCode) throws IOException {
        Document doc = documentBuilder.newDocument();

        Comment boilerplateComment = doc.createComment(Resources.toString(
                Resources.getResource("export-boilerplate.txt"), StandardCharsets.UTF_8));
        doc.appendChild(boilerplateComment);

        Country country = repository.getCountry();

        Element rootElem = doc.createElement("eag");
        rootElem.setAttribute("audience", "external");
        rootElem.setAttribute("xmlns", "http://www.archivesportaleurope.net/Portal/profiles/eag_2012/");
        rootElem.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        rootElem.setAttribute("xmlns:ape", "http://www.archivesportaleurope.eu/functions");
        rootElem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                "xs:schemaLocation",
                "http://www.archivesportaleurope.net/Portal/profiles/eag_2012/ http://schemas.archivesportaleurope.net/profiles/eag.xsd");
        doc.appendChild(rootElem);


        for (Description desc : LanguageHelpers.getBestDescription(
                repository, Optional.<Description>absent(), langCode).asSet()) {

            addControlSection(doc, rootElem, repository, country, desc);

            Element archGuideElem = doc.createElement("archguide");
            rootElem.appendChild(archGuideElem);

            addIdentitySection(doc, archGuideElem, desc);

            Element descElem = doc.createElement("desc");
            archGuideElem.appendChild(descElem);

            Element repositoriesElem = doc.createElement("repositories");
            descElem.appendChild(repositoriesElem);

            Element repoElem = doc.createElement("repository");
            repositoriesElem.appendChild(repoElem);

            Element geogAreaElem = doc.createElement("geogarea");
            geogAreaElem.setTextContent(LanguageHelpers.countryCodeToContinent(country.getCode())
                    .or("Europe")); // FIXME: Default???
            repoElem.appendChild(geogAreaElem);

            for (Address address : (desc.as(RepositoryDescription.class))
                    .getAddresses()) {
                Element locationElem = doc.createElement("location");
                locationElem.setAttribute("localType", "postal address");
                repoElem.appendChild(locationElem);
                Element countryElem = doc.createElement("country");
                locationElem.appendChild(countryElem);
                String cc = Optional.fromNullable(((String) address.getProperty("countryCode")))
                        .or(country.getCode());
                countryElem.setTextContent(new Locale("en", cc).getDisplayCountry());
                Element postCodeElem = doc.createElement("municipalityPostalcode");
                locationElem.appendChild(postCodeElem);
                postCodeElem.setTextContent((String) address.getProperty("postalcode"));

                Element streetElem = doc.createElement("street");
                locationElem.appendChild(streetElem);
                streetElem.setTextContent((String) address.getProperty("street"));

                for (String contact : new String[]{"telephone", "fax"}) {
                    for (Object strOrList : Optional.fromNullable(address.getProperty(contact)).asSet()) {
                        List values = strOrList instanceof List
                                ? (List) strOrList : Lists.newArrayList(strOrList);
                        for (Object value : values) {
                            Element elem = doc.createElement(contact);
                            elem.setTextContent(value.toString());
                            repoElem.appendChild(elem);
                        }
                    }
                }
                for (String ref : new String[]{"email", "webpage"}) {
                    for (Object strOrList : Optional.fromNullable(address.getProperty(ref)).asSet()) {
                        List values = strOrList instanceof List
                                ? (List) strOrList : Lists.newArrayList(strOrList);
                        for (Object value : values) {
                            Element elem = doc.createElement(ref);
                            elem.setAttribute("href", value.toString());
                            repoElem.appendChild(elem);
                        }
                    }
                }

            }

            addTextElements(doc, repoElem, desc, "history", "geoculturalContext",
                    "mandates", "buildings", "holdings");

            Element timetableElem = doc.createElement("timetable");
            repoElem.appendChild(timetableElem);
            Element openingElem = doc.createElement("opening");
            timetableElem.appendChild(openingElem);
            openingElem.setTextContent((String) desc.getProperty("openingTimes"));

            Element accessElem = doc.createElement("access");
            accessElem.setAttribute("question", "yes"); // ???
            repoElem.appendChild(accessElem);

            for (String terms : Optional.fromNullable(desc.<String>getProperty("conditions")).asSet()) {
                Element termsElem = doc.createElement("termsOfUse");
                accessElem.appendChild(termsElem);
                termsElem.setTextContent(terms);
            }

            Element accessibilityElem = doc.createElement("accessibility");
            accessibilityElem.setAttribute("question", "yes");
            accessibilityElem.setTextContent(((String) desc.getProperty("accessibility")));
            repoElem.appendChild(accessibilityElem);

            addTextElements(doc, repoElem, desc, "researchServices");
        }

        return doc;
    }

    private void addControlSection(Document doc, Element rootElem, Repository repository, Country country, Description desc) {
        Element controlElem = doc.createElement("control");
        rootElem.appendChild(controlElem);

        Element recordIdElem = doc.createElement("recordId");
        recordIdElem.setTextContent(String.format("%s-%s", country.getCode().toUpperCase(),
                repository.getIdentifier()));
        controlElem.appendChild(recordIdElem);

        Element otherRecordIdElem = doc.createElement("otherRecordId");
        otherRecordIdElem.setTextContent(repository.getId());
        otherRecordIdElem.setAttribute("localType", "yes");
        controlElem.appendChild(otherRecordIdElem);

        Element mainAgencyElem = doc.createElement("maintenanceAgency");
        Element agencyCodeElem = doc.createElement("agencyCode");
        agencyCodeElem.setTextContent("EHRI");
        Element agencyNameElem = doc.createElement("agencyName");
        agencyNameElem.setTextContent("The EHRI Consortium");
        mainAgencyElem.appendChild(agencyCodeElem);
        mainAgencyElem.appendChild(agencyNameElem);
        controlElem.appendChild(mainAgencyElem);

        Element mainStatusElem = doc.createElement("maintenanceStatus");
        mainStatusElem.setTextContent("revised");
        controlElem.appendChild(mainStatusElem);

        addRevisionDesc(doc, controlElem, repository, desc);
    }

    private void addIdentitySection(Document doc, Element archGuideElem, Description desc) {
        Element identityElem = doc.createElement("identity");
        archGuideElem.appendChild(identityElem);

        Element autFormElem = doc.createElement("autform");
        autFormElem.setAttribute("xml:lang", desc.getLanguageOfDescription());
        autFormElem.setTextContent(desc.getName());
        identityElem.appendChild(autFormElem);

        for (Object parNames : Optional.fromNullable(desc.getProperty("parallelFormsOfName")).asSet()) {
            List values = parNames instanceof List
                    ? (List) parNames
                    : Lists.newArrayList(parNames);
            for (Object value : values) {
                Element parFormElem = doc.createElement("parform");
                parFormElem.setTextContent(value.toString());
                identityElem.appendChild(parFormElem);
            }
        }
        for (Object parNames : Optional.fromNullable(desc.getProperty("otherFormsOfName")).asSet()) {
            List values = parNames instanceof List
                    ? (List) parNames
                    : Lists.newArrayList(parNames);
            for (Object value : values) {
                Element parFormElem = doc.createElement("parform");
                parFormElem.setTextContent(value.toString());
                identityElem.appendChild(parFormElem);
            }
        }
    }

    private void addRevisionDesc(Document doc, Element controlElem,
            Described entity, Description desc) {

        // NB: We could share all this horrible code with the EAC exporter
        // if the SCHEMAS DIDN'T HAVE THE ELEMENTS IN AN ARBITRARILY
        // DIFFERENT ORDER!!! Hurruph.

        Element revDescElem = doc.createElement("maintenanceHistory");
        controlElem.appendChild(revDescElem);

        List<MaintenanceEvent> maintenanceEvents = Lists
                .newArrayList(desc.getMaintenanceEvents());
        for (MaintenanceEvent event : maintenanceEvents) {
            Element eventElem = doc.createElement("maintenanceEvent");

            Element agent = doc.createElement("agent");
            agent.setTextContent("EHRI");
            eventElem.appendChild(agent);

            Element agentType = doc.createElement("agentType");
            agentType.setTextContent(MaintenanceEventAgentType.human.name());
            eventElem.appendChild(agentType);

            Element eventDateTime = doc.createElement("eventDateTime");
            eventDateTime.setTextContent((String) event.getProperty("date"));
            eventElem.appendChild(eventDateTime);

            Element eventType = doc.createElement("eventType");
            eventType.setTextContent(event.getEventType().name());
            eventElem.appendChild(eventType);

            revDescElem.appendChild(eventElem);
        }

        List<List<SystemEvent>> systemEvents = Lists.newArrayList(eventManager
                .aggregateForItem(entity));
        for (int i = systemEvents.size() - 1; i >= 0; i--) {
            List<SystemEvent> agg = systemEvents.get(i);
            SystemEvent event = agg.get(0);

            Element eventElem = doc.createElement("maintenanceEvent");

            Element agentElem = doc.createElement("agent");
            if (event.getActioner() != null) {
                agentElem.setTextContent(event.getActioner().getName());
            }
            eventElem.appendChild(agentElem);

            Element agentTypeElem = doc.createElement("agentType");
            agentTypeElem.setTextContent(MaintenanceEventAgentType.human.name());
            eventElem.appendChild(agentTypeElem);

            Element eventDateTimeElem = doc.createElement("eventDateTime");
            DateTime dateTime = new DateTime(event.getTimestamp());
            eventDateTimeElem.setTextContent(DateTimeFormat.longDateTime().print(dateTime));
            eventDateTimeElem.setAttribute("standardDateTime", dateTime.toString());
            eventElem.appendChild(eventDateTimeElem);

            Element eventTypeElem = doc.createElement("eventType");
            eventTypeElem.setTextContent(MaintenanceEventType
                    .fromSystemEventType(event.getEventType()).name());
            eventElem.appendChild(eventTypeElem);

            revDescElem.appendChild(eventElem);
        }

        // We must provide a default event
        if (maintenanceEvents.isEmpty() && systemEvents.isEmpty()) {
            logger.debug("No events found for element {}, using fallback", entity.getId());
            Element eventElem = doc.createElement("maintenanceEvent");

            Element agentElem = doc.createElement("agent");
            agentElem.setTextContent(entity.getId());
            eventElem.appendChild(agentElem);

            Element agentTypeElem = doc.createElement("agentType");
            agentTypeElem.setTextContent(MaintenanceEventAgentType.machine.name());
            eventElem.appendChild(agentTypeElem);

            Element eventDateTimeElem = doc.createElement("eventDateTime");
            DateTime dateTime = DateTime.now();
            eventDateTimeElem.setTextContent(DateTimeFormat.longDateTime().print(dateTime));
            eventDateTimeElem.setAttribute("standardDateTime", dateTime.toString());
            eventElem.appendChild(eventDateTimeElem);

            Element eventTypeElem = doc.createElement("eventType");
            eventTypeElem.setTextContent(MaintenanceEventType.created.name());
            eventElem.appendChild(eventTypeElem);

            revDescElem.appendChild(eventElem);
        }
    }

    private void addTextElements(Document doc, Element parent, Description desc, String... toAdd)
            throws IOException {
        Set<String> adding = Sets.newHashSet(toAdd);
        for (Map.Entry<String, String> entry : descriptiveTextMappings.entrySet()) {
            if (adding.contains(entry.getKey())) {
                for (String prop : Optional.fromNullable(desc.<String>getProperty(entry.getKey())).asSet()) {
                    String elemName = entry.getValue();
                    Element topElem = parent;
                    if (elemName.contains("/")) {
                        List<String> strings = Splitter.on("/").splitToList(elemName);
                        for (int i = 0; i < strings.size() - 1; i++) {
                            Element next = doc.createElement(strings.get(i));
                            topElem.appendChild(next);
                            topElem = next;
                        }
                        elemName = strings.get(strings.size() - 1);
                    }

                    NodeList elements = parent.getElementsByTagName(elemName);
                    Element descNoteElem;
                    if (elements.getLength() == 0) {
                        Element propElem = (Element) topElem
                                .appendChild(doc.createElement(elemName));
                        descNoteElem = doc.createElement("descriptiveNote");
                        propElem.appendChild(descNoteElem);
                    } else {
                        descNoteElem = (Element) elements.item(0).getFirstChild();
                    }

                    createCDataElement(doc, descNoteElem, "p", prop);
                }
            }
        }
    }
}
