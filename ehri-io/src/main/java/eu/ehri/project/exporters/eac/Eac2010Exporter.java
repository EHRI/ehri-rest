/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Isaar;
import eu.ehri.project.exporters.xml.AbstractStreamingXmlExporter;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventAgentType;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Named;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.utils.LanguageHelpers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Export EAC 2010 XML.
 */
public final class Eac2010Exporter extends AbstractStreamingXmlExporter<HistoricalAgent> implements EacExporter {

    private static final Logger logger = LoggerFactory.getLogger(Eac2010Exporter.class);
    private static final Map<String, String> NAMESPACES = namespaces(
            "xlink", "http://www.w3.org/1999/xlink",
            "xsi", "http://www.w3.org/2001/XMLSchema-instance"
    );
    private static final String DEFAULT_NAMESPACE = "urn:isbn:1-931666-33-4";

    private final Api api;

    private static final ImmutableMap<Isaar, String> descriptiveTextMappings = ImmutableMap.<Isaar, String>builder()
            .put(Isaar.place, "place/placeEntry")
            .put(Isaar.legalStatus, "legalStatus/term")
            .put(Isaar.functions, "function/term")
            .put(Isaar.occupation, "occupation/term")
            .put(Isaar.mandates, "mandate/term")
            .build();

    private static final ImmutableMap<Isaar, String> pureTextMappings = ImmutableMap.<Isaar, String>builder()
            .put(Isaar.structure, "structureOrGenealogy")
            .put(Isaar.generalContext, "generalContext")
            .put(Isaar.biographicalHistory, "biogHist")
            .build();

    private static final ImmutableMap<Isaar, String> nameMappings = ImmutableMap.of(
            Isaar.lastName, "lastname",
            Isaar.firstName, "forename");

    public Eac2010Exporter(Api api) {
        this.api = api;
    }

    @Override
    public void export(XMLStreamWriter sw, HistoricalAgent agent, String langCode) {
        comment(sw, resourceAsString("export-boilerplate.txt"));
        root(sw, "eac-cpf", DEFAULT_NAMESPACE, attrs(), NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", DEFAULT_NAMESPACE + "http://eac.staatsbibliothek-berlin.de/schema/cpf.xsd");
            LanguageHelpers.getBestDescription(agent, Optional.empty(), langCode).ifPresent(desc -> {

                addControlSection(sw, agent, desc);

                tag(sw, "cpfDescription", () -> {

                    addIdentitySection(sw, agent, desc);

                    tag(sw, "description", () -> {

                        addDatesOfExistence(sw, desc);

                        for (Map.Entry<Isaar, String> entry : descriptiveTextMappings.entrySet()) {
                            addTextElements(sw, desc, entry.getKey().name(), entry.getValue());
                        }
                        for (Map.Entry<Isaar, String> entry : pureTextMappings.entrySet()) {
                            addPureTextElements(sw, desc, entry.getKey().name(), entry.getValue());
                        }
                    });

                    addRelations(sw, agent, desc, langCode);
                });
            });
        });
    }

    private void addRelations(XMLStreamWriter sw, HistoricalAgent agent, Description desc, String langCode) {
        List<Link> linkRels = ImmutableList.copyOf(agent.getLinks());
        if (!linkRels.isEmpty()) {
            tag(sw, "relations", () -> {
                for (Link link : linkRels) {
                    // FIXME: Harmonise this attribute
                    tag(sw, "cpfRelation", attrs("cpfRelationType", "associative"), () -> {

                        // Look for a body which is an access point
                        getLinkEntityId(agent, link).ifPresent(id ->
                                attribute(sw, "http://www.w3.org/1999/xlink", "href", id)
                        );
                        getLinkName(agent, desc, link, langCode).ifPresent(name ->
                                tag(sw, "relationEntry", name)
                        );
                        getLinkDescription(link).ifPresent(name ->
                                tag(sw, ImmutableList.of("descriptiveNote", "p"), () -> cData(sw, name))
                        );
                    });
                }
            });
        }
    }

    private void addPureTextElements(XMLStreamWriter sw, Description desc, String key, String path) {
        Optional.ofNullable(desc.getProperty(key)).ifPresent(prop ->
                tag(sw, ImmutableList.of(path, "p"), prop.toString())
        );
    }

    private void addTextElements(XMLStreamWriter sw, Description desc, String key, String path) {
        Optional.ofNullable(desc.getProperty(key)).ifPresent(prop -> {
            List<String> keys = Splitter.on("/").splitToList(path);
            for (Object value : coerceList(prop)) {
                tag(sw, keys, value.toString());
            }
        });
    }

    private void addDatesOfExistence(XMLStreamWriter sw, Description desc) {
        List<DatePeriod> allDates = ImmutableList.copyOf(desc.as(HistoricalAgentDescription.class)
                .getDatePeriods());
        List<DatePeriod> existence = allDates.stream()
                .filter(d -> DatePeriod.DatePeriodType.existence.equals(d.getDateType()))
                .collect(Collectors.toList());
        if (existence.isEmpty() && !allDates.isEmpty()) {
            existence.add(allDates.get(0));
        }

        String datesOfExistence = desc.getProperty("datesOfExistence");
        if (!existence.isEmpty() || datesOfExistence != null) {
            tag(sw, "existDates", () -> {
                for (DatePeriod datePeriod : existence) {
                    tag(sw, "dateRange", () -> {
                        String startDate = datePeriod.getStartDate();
                        String endDate = datePeriod.getEndDate();
                        if (startDate != null) {
                            String startYear = String.valueOf(new DateTime(startDate).year().get());
                            tag(sw, "fromDate", startYear, attrs("standardDate", startYear));
                        }
                        if (endDate != null) {
                            String endYear = String.valueOf(new DateTime(endDate).year().get());
                            tag(sw, "toDate", endYear, attrs("standardDate", endYear));
                        }
                    });
                }
                if (existence.isEmpty() && datesOfExistence != null) {
                    tag(sw, "date", () -> cData(sw, datesOfExistence));
                } else if (datesOfExistence != null) {
                    tag(sw, ImmutableList.of("descriptiveNote", "p"),
                            () -> cData(sw, datesOfExistence));
                }
            });
        }
    }

    private void addIdentitySection(XMLStreamWriter sw, HistoricalAgent agent, Description desc) {
        tag(sw, "identity", () -> {
            tag(sw, "entityId", agent.getIdentifier());
            tag(sw, "entityType", desc.<String>getProperty(Isaar.typeOfEntity));
            tag(sw, "nameEntry", () -> {
                tag(sw, "part", desc.getName());
                tag(sw, "authorizedForm", "ehri");
            });

            for (Map.Entry<Isaar, String> entry : nameMappings.entrySet()) {
                Optional.ofNullable(desc.<String>getProperty(entry.getKey())).ifPresent(value ->
                        tag(sw, ImmutableList.of("nameEntry", "part"),
                                value, attrs("localType", entry.getValue()))
                );
            }

            Optional.ofNullable(desc.getProperty(Isaar.otherFormsOfName)).ifPresent(parNames -> {
                List<?> values = coerceList(parNames);
                if (!values.isEmpty()) {
                    for (Object value : values) {
                        tag(sw, "nameEntry", () -> {
                            tag(sw, "part", value.toString());
                            tag(sw, "alternativeForm", "ehri");
                        });
                    }
                }
            });

            Optional.ofNullable(desc.getProperty(Isaar.parallelFormsOfName)).ifPresent(parNames -> {
                List<?> values = coerceList(parNames);
                if (!values.isEmpty()) {
                    tag(sw, "nameEntryParallel", () -> {
                        tag(sw, "nameEntry", () -> {
                            tag(sw, "part", desc.getName());
                            tag(sw, "preferredForm", "ehri");
                        });
                        for (Object value : values) {
                            tag(sw, ImmutableList.of("nameEntry", "part"), value.toString());
                        }
                    });
                }
            });
        });
    }

    private void addControlSection(XMLStreamWriter sw, HistoricalAgent agent, Description desc) {
        tag(sw, "control", () -> {
            tag(sw, "recordId", agent.getId());
            tag(sw, "otherRecordId", agent.getIdentifier(), attrs("localType", "yes"));
            tag(sw, "maintenanceStatus", "revised");
            tag(sw, "publicationStatus", "approved"); // FIXME?
            tag(sw, ImmutableList.of("maintenanceAgency", "agencyName"), "The EHRI Consortium");
            tag(sw, "languageDeclaration", () -> {
                tag(sw, "language", LanguageHelpers.codeToName(desc.getLanguageOfDescription()),
                        attrs("languageCode", desc.getLanguageOfDescription()));
                // NB: Assume script is Latin!!!
                tag(sw, "script", "Latin", attrs("scriptCode", "Latn"));
            });
            tag(sw, "conventionDeclaration", () -> {
                tag(sw, "abbreviation", "ehri");
                tag(sw, "citation", "EHRI Naming Policy");
            });

            addRevisionDesc(sw, agent, desc);

            Optional.ofNullable(desc.getProperty(Isaar.source)).ifPresent(sources -> {
                List<?> sourceValues = coerceList(sources);
                tag(sw, "sources", () -> {
                    for (Object value : sourceValues) {
                        tag(sw, "source", () -> tag(sw, "sourceEntry", value.toString()));
                    }
                });
            });
        });
    }

    private void addRevisionDesc(XMLStreamWriter sw, HistoricalAgent agent, Description desc) {
        tag(sw, "maintenanceHistory", () -> {
            List<MaintenanceEvent> maintenanceEvents = Lists
                    .newArrayList(desc.getMaintenanceEvents());
            for (MaintenanceEvent event : maintenanceEvents) {
                tag(sw, "maintenanceEvent", () -> {
                    tag(sw, "eventType", event.getEventType().name());
                    // TODO: Normalise and put standardDateTime attribute here?
                    tag(sw, "eventDateTime", event.<String>getProperty("date"));
                    tag(sw, "agentType", MaintenanceEventAgentType.human.name());
                    tag(sw, "agent", "EHRI");
                    String eventDesc = event.getProperty("source");
                    if (eventDesc != null && !eventDesc.trim().isEmpty()) {
                        tag(sw, "eventDescription", eventDesc);
                    }
                });
            }

            List<List<SystemEvent>> systemEvents = ImmutableList.copyOf(
                    api.events().aggregateForItem(agent));
            for (List<SystemEvent> events : Lists.reverse(systemEvents)) {
                tag(sw, "maintenanceEvent", () -> {
                    SystemEvent event = events.get(0);

                    tag(sw, "eventType", MaintenanceEventType
                            .fromSystemEventType(event.getEventType()).name());
                    DateTime dateTime = new DateTime(event.getTimestamp());
                    tag(sw, "eventDateTime", DateTimeFormat.longDateTime().print(dateTime),
                            attrs("standardDateTime", dateTime.toString()));
                    tag(sw, "agentType", MaintenanceEventAgentType.human.name());
                    tag(sw, "agent", Optional.ofNullable(event.getActioner())
                            .map(Named::getName).orElse("EHRI"));
                    if (event.getLogMessage() != null && !event.getLogMessage().isEmpty()) {
                        tag(sw, "eventDescription", event.getLogMessage());
                    }
                });
            }

            // We must provide a default event
            if (maintenanceEvents.isEmpty() && systemEvents.isEmpty()) {
                logger.debug("No events found for element {}, using fallback", agent.getId());
                tag(sw, "maintenanceEvent", () -> {
                    tag(sw, "eventType", MaintenanceEventType.created.name());
                    DateTime dateTime = DateTime.now();
                    tag(sw, "eventDateTime", DateTimeFormat.longDateTime().print(dateTime),
                            attrs("standardDateTime", dateTime.toString()));
                    tag(sw, "agentType", MaintenanceEventAgentType.machine.name());
                    tag(sw, "agent", agent.getId());
                });
            }
        });
    }

    private Optional<String> getLinkDescription(Link link) {
        String desc = link.getDescription();
        if (desc == null) {
            for (Accessible other : link.getLinkBodies()) {
                if (other.getType().equals(Entities.ACCESS_POINT)) {
                    AccessPoint ap = other.as(AccessPoint.class);
                    desc = ap.getProperty("description");
                }
            }
        }
        if (desc != null && !desc.trim().isEmpty()) {
            return Optional.of(desc);
        }
        return Optional.empty();
    }

    private Optional<String> getLinkName(Described entity,
                                         Description description, Link link, String lang) {
        for (Accessible other : link.getLinkBodies()) {
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
        for (Accessible other : link.getLinkTargets()) {
            if (!other.equals(entity)) {
                return Optional.of(getEntityName(other.as(Described.class), lang));
            }
        }

        return Optional.empty();
    }

    private Optional<String> getLinkEntityId(Described entity, Link link) {
        for (Accessible other : link.getLinkTargets()) {
            if (!other.equals(entity)) {
                return Optional.of(other.getId());
            }
        }
        return Optional.empty();
    }

    private String getEntityName(Described entity, String lang) {
        return LanguageHelpers.getBestDescription(entity, lang)
                .map(Description::getName)
                .orElse(entity.getIdentifier()); // Fallback
    }
}
