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

package eu.ehri.project.exporters.eac;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exporters.DocumentWriter;
import eu.ehri.project.exporters.util.Helpers;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventAgentType;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.views.EventViews;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static eu.ehri.project.exporters.util.Helpers.createCDataElement;

/**
 * Export EAC 2010 XML.
 */
public class Eac2010Exporter implements EacExporter {
    private static final Logger logger = LoggerFactory.getLogger(Eac2010Exporter.class);

    protected final FramedGraph<?> framedGraph;
    protected final EventViews eventManager;
    protected final DocumentBuilder documentBuilder;

    private static final ImmutableMap<String, String> descriptiveTextMappings = ImmutableMap.<String, String>builder()
            .put("place", "places/place/placeEntry")
            .put("legalStatus", "legalStatus/term")
            .put("functions", "function/term")
            .put("occupation", "occupation/term")
            .put("mandates", "mandate/term")
            .build();

    private static final ImmutableMap<String, String> pureTextMappings = ImmutableMap.<String, String>builder()
            .put("structure", "structureOrGenealogy")
            .put("generalContext", "generalContext")
            .put("biographicalHistory", "biogHist")
            .build();

    public Eac2010Exporter(final FramedGraph<?> framedGraph) {
        this.framedGraph = framedGraph;
        eventManager = new EventViews(framedGraph);
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void export(HistoricalAgent agent, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        new DocumentWriter(export(agent, langCode)).write(outputStream);
    }

    @Override
    public Document export(final HistoricalAgent agent, String langCode) throws IOException {
        Document doc = documentBuilder.newDocument();
        Comment boilerplateComment = doc.createComment(Resources.toString(
                Resources.getResource("export-boilerplate.txt"), StandardCharsets.UTF_8));
        doc.appendChild(boilerplateComment);

        Element rootElem = doc.createElement("eac-cpf");
        rootElem.setAttribute("xmlns", "urn:isbn:1-931666-33-4");
        rootElem.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        rootElem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                "xs:schemaLocation",
                "urn:isbn:1-931666-33-4 http://eac.staatsbibliothek-berlin.de/schema/cpf.xsd");
        doc.appendChild(rootElem);

        for (Description desc : Helpers.getBestDescription(
                agent, Optional.<Description>absent(), langCode).asSet()) {

            addControlSection(agent, doc, rootElem, desc);

            Element cpfDescElem = doc.createElement("cpfDescription");
            rootElem.appendChild(cpfDescElem);

            addIdentitySection(agent, desc, doc, cpfDescElem);

            Element descElem = doc.createElement("description");
            cpfDescElem.appendChild(descElem);

            addDatesOfExistence(doc, descElem, desc);

            for (Map.Entry<String, String> entry : descriptiveTextMappings.entrySet()) {
                addTextElements(doc, descElem, desc, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : pureTextMappings.entrySet()) {
                addPureTextElements(doc, descElem, desc, entry.getKey(), entry.getValue());
            }

            addRelations(doc, cpfDescElem, agent, desc, langCode);
        }

        return doc;
    }

    private void addRelations(Document doc, Element cpfDesc, HistoricalAgent agent,
            Description desc, String langCode) {

        List<Link> linkRels = Lists.newArrayList(agent.getLinks());
        if (!linkRels.isEmpty()) {
            Element relationsElem = doc.createElement("relations");
            cpfDesc.appendChild(relationsElem);

            for (Link link : linkRels) {

                Element cpfRelElem = doc.createElement("cpfRelation");
                // FIXME: Harmonise this attribute
                cpfRelElem.setAttribute("cpfRelationType", "associative");
                relationsElem.appendChild(cpfRelElem);

                // Look for a body which is an access point
                for (String id : getLinkEntityId(agent, link).asSet()) {
                    cpfRelElem.setAttribute("xlink:href", id);
                }
                for (String name : getLinkName(agent, desc, link, langCode).asSet()) {
                    Element cpfRelEntry = doc.createElement("relationEntry");
                    cpfRelEntry.setTextContent(name);
                    cpfRelElem.appendChild(cpfRelEntry);
                }
                for (String name : getLinkDescription(link).asSet()) {
                    Element descNote = doc.createElement("descriptiveNote");
                    createCDataElement(doc, descNote, "p", name);
                    cpfRelElem.appendChild(descNote);
                }
            }
        }
    }

    private void addDatesOfExistence(Document doc, Element descElem, Description desc) {
        List<DatePeriod> allDates = Lists.newArrayList(desc.as(HistoricalAgentDescription.class)
                .getDatePeriods());
        List<DatePeriod> existence = Lists.newArrayList();
        for (DatePeriod datePeriod : allDates) {
            if (DatePeriod.DatePeriodType.existence.equals(datePeriod.getDateType())) {
                existence.add(datePeriod);
            }
        }
        if (existence.isEmpty() && !allDates.isEmpty()) {
            existence.add(allDates.get(0));
        }

        String datesOfExistence = desc.getProperty("datesOfExistence");
        if (!existence.isEmpty() || datesOfExistence != null) {
            Element existDates = doc.createElement("existDates");
            descElem.appendChild(existDates);

            for (DatePeriod datePeriod : existence) {
                Element dpElem = doc.createElement("dateRange");
                existDates.appendChild(dpElem);

                String startDate = datePeriod.getStartDate();
                String endDate = datePeriod.getEndDate();

                if (startDate != null) {
                    Element start = doc.createElement("fromDate");
                    String startYear = String.valueOf(new DateTime(startDate).year().get());
                    start.setAttribute("standardDate", startYear);
                    start.setTextContent(startYear);
                    dpElem.appendChild(start);
                }

                if (endDate != null) {
                    Element end = doc.createElement("toDate");
                    String endYear = String.valueOf(new DateTime(endDate).year().get());
                    end.setAttribute("standardDate", endYear);
                    end.setTextContent(endYear);
                    dpElem.appendChild(end);
                }
            }

            if (existence.isEmpty() && datesOfExistence != null) {
                Element date = doc.createElement("date");
                existDates.appendChild(date);
                date.setTextContent(datesOfExistence);
            } else if (datesOfExistence != null) {
                Element note = doc.createElement("descriptiveNote");
                existDates.appendChild(note);
                createCDataElement(doc, note, "p", datesOfExistence);
            }
        }
    }

    private void addIdentitySection(HistoricalAgent agent, Description desc, Document doc, Element cpfDescElem) {
        Element identityElem = doc.createElement("identity");

        Element entityIdElem = doc.createElement("entityId");
        entityIdElem.setTextContent(agent.getIdentifier());
        identityElem.appendChild(entityIdElem);

        cpfDescElem.appendChild(identityElem);

        Element entityTypeElem = doc.createElement("entityType");
        entityTypeElem.setTextContent((String) desc.getProperty("typeOfEntity"));
        identityElem.appendChild(entityTypeElem);

        Element nameElem = doc.createElement("nameEntry");
        identityElem.appendChild(nameElem);

        Element namePartElem = doc.createElement("part");
        namePartElem.setTextContent(desc.getName());
        nameElem.appendChild(namePartElem);
        Element autFormElem = doc.createElement("authorizedForm");
        autFormElem.setTextContent("ehri");
        nameElem.appendChild(autFormElem);

        for (Map.Entry<String, String> entry : ImmutableMap.of(
                "lastName", "lastname", "firstName", "forename").entrySet()) {
            for (String value : Optional.fromNullable(desc.<String>getProperty(entry.getKey())).asSet()) {
                Element subNameElem = doc.createElement("nameEntry");
                identityElem.appendChild(subNameElem);
                Element nameLastElem = doc.createElement("part");
                nameLastElem.setAttribute("localType", entry.getValue());
                nameLastElem.setTextContent(value);
                subNameElem.appendChild(nameLastElem);
            }
        }

        for (Object parNames : Optional.fromNullable(desc.getProperty("otherFormsOfName")).asSet()) {
            List values = parNames instanceof List
                    ? (List) parNames
                    : Lists.newArrayList(parNames);
            if (!values.isEmpty()) {
                for (Object value : values) {
                    Element nameAltElem = doc.createElement("nameEntry");
                    identityElem.appendChild(nameAltElem);

                    Element nameAltPartElem = doc.createElement("part");
                    nameAltPartElem.setTextContent(value.toString());
                    nameAltElem.appendChild(nameAltPartElem);

                    Element autFormParElem = doc.createElement("alternativeForm");
                    autFormParElem.setTextContent("ehri");
                    nameAltElem.appendChild(autFormParElem);
                }
            }
        }

        for (Object parNames : Optional.fromNullable(desc.getProperty("parallelFormsOfName")).asSet()) {
            List values = parNames instanceof List
                    ? (List) parNames
                    : Lists.newArrayList(parNames);
            if (!values.isEmpty()) {
                Element nameEntryParElem = doc.createElement("nameEntryParallel");
                identityElem.appendChild(nameEntryParElem);

                Element nameParAutElem = doc.createElement("nameEntry");
                nameEntryParElem.appendChild(nameParAutElem);
                Element nameParAutPartElem = doc.createElement("part");
                nameParAutPartElem.setTextContent(desc.getName());
                nameParAutElem.appendChild(nameParAutPartElem);
                Element nameParAutPrefElem = doc.createElement("preferredForm");
                nameParAutPrefElem.setTextContent("ehri");
                nameParAutElem.appendChild(nameParAutPrefElem);

                for (Object value : values) {
                    Element nameParElem = doc.createElement("nameEntry");
                    nameEntryParElem.appendChild(nameParElem);

                    Element parFormElem = doc.createElement("part");
                    parFormElem.setTextContent(value.toString());
                    nameParElem.appendChild(parFormElem);
                }
            }
        }
    }

    private void addControlSection(HistoricalAgent agent, Document doc, Element root, Description desc) {
        Element controlElem = doc.createElement("control");
        root.appendChild(controlElem);

        Element recordIdElem = doc.createElement("recordId");
        recordIdElem.setTextContent(agent.getId());
        controlElem.appendChild(recordIdElem);

        Element otherRecordIdElem = doc.createElement("otherRecordId");
        otherRecordIdElem.setTextContent(agent.getIdentifier());
        otherRecordIdElem.setAttribute("localType", "yes");
        controlElem.appendChild(otherRecordIdElem);

        Element mainStatusElem = doc.createElement("maintenanceStatus");
        mainStatusElem.setTextContent("revised");
        controlElem.appendChild(mainStatusElem);

        Element pubStatusElem = doc.createElement("publicationStatus");
        pubStatusElem.setTextContent("approved"); // FIXME
        controlElem.appendChild(pubStatusElem);

        Element mainAgencyElem = doc.createElement("maintenanceAgency");
        Element agencyNameElem = doc.createElement("agencyName");
        agencyNameElem.setTextContent("The EHRI Consortium");
        mainAgencyElem.appendChild(agencyNameElem);
        controlElem.appendChild(mainAgencyElem);

        Element langDecElem = doc.createElement("languageDeclaration");
        controlElem.appendChild(langDecElem);
        Element lang = doc.createElement("language");
        lang.setAttribute("languageCode", desc.getLanguageOfDescription());
        lang.setTextContent(eu.ehri.project.importers.util
                .Helpers.codeToName(desc.getLanguageOfDescription()));
        langDecElem.appendChild(lang);

        // Add a convention declaration for EHRI's name policy
        Element nameConvDecElem = doc.createElement("conventionDeclaration");
        controlElem.appendChild(nameConvDecElem);
        Element nameConvAbbrElem = doc.createElement("abbreviation");
        nameConvAbbrElem.setTextContent("ehri");
        nameConvDecElem.appendChild(nameConvAbbrElem);
        Element nameConvCitElem = doc.createElement("citation");
        nameConvCitElem.setTextContent("EHRI Naming Policy");
        nameConvDecElem.appendChild(nameConvCitElem);

        // NB: Assume script is Latin!!!
        Element scriptElem = doc.createElement("script");
        scriptElem.setAttribute("scriptCode", "Latn");
        scriptElem.setTextContent("Latin");
        langDecElem.appendChild(scriptElem);

        addRevisionDesc(doc, controlElem, agent, desc);

        for (Object sources : Optional.fromNullable(desc.getProperty("source")).asSet()) {
            List sourceValues = sources instanceof List
                    ? (List) sources
                    : Lists.newArrayList(sources);
            Element sourcesElem = doc.createElement("sources");
            controlElem.appendChild(sourcesElem);
            for (Object value : sourceValues) {
                Element sourceElem = doc.createElement("source");
                sourcesElem.appendChild(sourceElem);
                Element sourceEntryElem = doc.createElement("sourceEntry");
                sourceElem.appendChild(sourceEntryElem);
                sourceEntryElem.setTextContent(value.toString());
            }
        }
    }


    private void addRevisionDesc(Document doc, Element controlElem, DescribedEntity entity, Description desc) {

        Element mainHistElem = doc.createElement("maintenanceHistory");
        controlElem.appendChild(mainHistElem);

        // Pre-ingest events
        List<MaintenanceEvent> maintenanceEvents = Lists
                .newArrayList(desc.getMaintenanceEvents());
        for (MaintenanceEvent event : maintenanceEvents) {
            Element eventElem = doc.createElement("maintenanceEvent");

            Element eventTypeElem = doc.createElement("eventType");
            eventTypeElem.setTextContent(event.getEventType().name());
            eventElem.appendChild(eventTypeElem);

            Element eventDateTimeElem = doc.createElement("eventDateTime");
            // TODO: Normalise and put standardDateTime attribute here?
            eventDateTimeElem.setTextContent((String) event.getProperty("maintenanceEvent/date"));
            eventElem.appendChild(eventDateTimeElem);

            Element agentTypeElem = doc.createElement("agentType");
            agentTypeElem.setTextContent(MaintenanceEventAgentType.human.name());
            eventElem.appendChild(agentTypeElem);

            Element agentElem = doc.createElement("agent");
            agentElem.setTextContent("EHRI");
            eventElem.appendChild(agentElem);

            String eventDesc = event.getProperty("maintenanceEvent/source");
            if (eventDesc != null && !eventDesc.trim().isEmpty()) {
                Element eventDescElem = doc.createElement("eventDescription");
                eventElem.appendChild(eventDescElem);
                eventDescElem.setTextContent(eventDesc);
            }

            mainHistElem.appendChild(eventElem);
        }

        // Post-ingest events
        List<List<SystemEvent>> systemEvents = Lists.newArrayList(eventManager
                .aggregateForItem(entity, AnonymousAccessor.getInstance()));
        for (int i = systemEvents.size() - 1; i >= 0; i--) {
            List<SystemEvent> agg = systemEvents.get(i);
            SystemEvent event = agg.get(0);

            Element eventElem = doc.createElement("maintenanceEvent");

            Element eventTypeElem = doc.createElement("eventType");
            eventTypeElem.setTextContent(MaintenanceEventType
                    .fromSystemEventType(event.getEventType()).name());
            eventElem.appendChild(eventTypeElem);

            Element eventDateTimeElem = doc.createElement("eventDateTime");
            DateTime dateTime = new DateTime(event.getTimestamp());
            eventDateTimeElem.setTextContent(DateTimeFormat.longDateTime().print(dateTime));
            eventDateTimeElem.setAttribute("standardDateTime", dateTime.toString());
            eventElem.appendChild(eventDateTimeElem);

            Element agentTypeElem = doc.createElement("agentType");
            agentTypeElem.setTextContent(MaintenanceEventAgentType.human.name());
            eventElem.appendChild(agentTypeElem);

            Element agentElem = doc.createElement("agent");
            if (event.getActioner() != null) {
                agentElem.setTextContent(event.getActioner().getName());
            }
            eventElem.appendChild(agentElem);

            if (event.getLogMessage() != null && !event.getLogMessage().isEmpty()) {
                Element eventDescElem = doc.createElement("eventDescription");
                eventElem.appendChild(eventDescElem);
                eventDescElem.setTextContent(event.getLogMessage());
            }

            mainHistElem.appendChild(eventElem);
        }

        // We must provide a default event
        if (maintenanceEvents.isEmpty() && systemEvents.isEmpty()) {
            logger.debug("No events found for element {}, using fallback", entity.getId());
            Element eventElem = doc.createElement("maintenanceEvent");

            Element eventTypeElem = doc.createElement("eventType");
            eventTypeElem.setTextContent(MaintenanceEventType.created.name());
            eventElem.appendChild(eventTypeElem);

            Element eventDateTimeElem = doc.createElement("eventDateTime");
            DateTime dateTime = DateTime.now();
            eventDateTimeElem.setTextContent(DateTimeFormat.longDateTime().print(dateTime));
            eventDateTimeElem.setAttribute("standardDateTime", dateTime.toString());
            eventElem.appendChild(eventDateTimeElem);

            Element agentTypeElem = doc.createElement("agentType");
            agentTypeElem.setTextContent(MaintenanceEventAgentType.machine.name());
            eventElem.appendChild(agentTypeElem);

            Element agentElem = doc.createElement("agent");
            agentElem.setTextContent(entity.getId());
            eventElem.appendChild(agentElem);

            mainHistElem.appendChild(eventElem);
        }
    }

    protected Optional<String> getLinkDescription(Link link) {
        String desc = link.getDescription();
        if (desc == null) {
            for (AccessibleEntity other : link.getLinkBodies()) {
                if (other.getType().equals(Entities.ACCESS_POINT)) {
                    AccessPoint ap = other.as(AccessPoint.class);
                    desc = ap.getProperty("description");
                }
            }
        }
        if (desc != null && !desc.trim().isEmpty()) {
            return Optional.of(desc);
        }
        return Optional.absent();
    }

    protected Optional<String> getLinkName(DescribedEntity entity,
            Description description, Link link, String lang) {
        for (AccessibleEntity other : link.getLinkBodies()) {
            // We only use an access point body for the name of this link
            // if the access point is on the current entity (otherwise the
            // link will have the same name as our current item.)
            if (other.getType().equals(Entities.ACCESS_POINT)) {
                AccessPoint ap = other.as(AccessPoint.class);
                for (AccessPoint outAp : description.getAccessPoints()) {
                    if (outAp.equals(ap)) {
                        return Optional.of(ap.getName());
                    }
                }
            }
        }
        for (AccessibleEntity other : link.getLinkTargets()) {
            if (!other.equals(entity)) {
                return Optional.of(getEntityName(other.as(DescribedEntity.class), lang));
            }
        }

        return Optional.absent();
    }

    protected Optional<String> getLinkEntityId(DescribedEntity entity, Link link) {
        for (AccessibleEntity other : link.getLinkTargets()) {
            if (!other.equals(entity)) {
                return Optional.of(other.getId());
            }
        }
        return Optional.absent();
    }

    protected String getEntityName(DescribedEntity entity, String lang) {
        return Helpers.getBestDescription(entity, lang).transform(new Function<Description, String>() {
            @Override
            public String apply(Description description) {
                return description.getName();
            }
        }).or(entity.getIdentifier()); // Fallback
    }

    private void addTextElements(Document doc, Element parent, Description desc, String key, String path)
            throws IOException {
        for (Object prop : Optional.fromNullable(desc.getProperty(key)).asSet()) {
            String elemName = path;
            Element top = parent;
            if (elemName.contains("/")) {
                List<String> strings = Splitter.on("/").splitToList(elemName);
                for (int i = 0; i < strings.size() - 1; i++) {
                    Element next = doc.createElement(strings.get(i));
                    top.appendChild(next);
                    top = next;
                }
                elemName = strings.get(strings.size() - 1);
            }
            List values = prop instanceof List
                    ? (List) prop
                    : Lists.newArrayList(prop);
            for (Object value : values) {
                Element propElem = (Element) top.appendChild(doc.createElement(elemName));
                propElem.setTextContent(value.toString());
            }
        }
    }

    private void addPureTextElements(Document doc, Element parent, Description desc, String key, String path)
            throws IOException {
        for (Object prop : Optional.fromNullable(desc.getProperty(key)).asSet()) {
            Element propElem = doc.createElement(path);
            parent.appendChild(propElem);
            createCDataElement(doc, propElem, "p", prop.toString());
        }
    }
}
