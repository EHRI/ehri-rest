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

package eu.ehri.project.exporters.eag;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.ContactInfo;
import eu.ehri.project.definitions.Isdiah;
import eu.ehri.project.exporters.xml.AbstractStreamingXmlExporter;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventAgentType;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Export EAG 2012 XML.
 */
public final class Eag2012Exporter extends AbstractStreamingXmlExporter<Repository> implements EagExporter {
    private static final Logger logger = LoggerFactory.getLogger(Eag2012Exporter.class);
    private static final String DEFAULT_NAMESPACE = "http://www.archivesportaleurope.net/Portal/profiles/eag_2012/";

    private final Api api;

    private static final Map<Isdiah, String> descriptiveTextMappings = ImmutableMap.<Isdiah, String>builder()
            .put(Isdiah.buildings, "buildinginfo/building")
            .put(Isdiah.holdings, "holdings")
            .put(Isdiah.conditions, "termsOfUse") // added to mandatory access
            .put(Isdiah.researchServices, "services/searchroom/researchServices")
            .build();

    private static final List<Isdiah> historyElements = ImmutableList.of(
            Isdiah.history, Isdiah.geoculturalContext, Isdiah.mandates);

    private static final Map<String, String> NAMESPACES = namespaces(
            "xlink", "http://www.w3.org/1999/xlink",
            "ape", "http://www.archivesportaleurope.eu/functions",
            "xsi", "http://www.w3.org/2001/XMLSchema-instance");

    private static final Map<String, String> ATTRS = attrs(
            "audience", "external"
    );

    public Eag2012Exporter(final Api api) {
        this.api = api;
    }

    @Override
    public void export(XMLStreamWriter sw, Repository repository, String langCode) {

        comment(sw, resourceAsString("export-boilerplate.txt"));

        Country country = repository.getCountry();

        root(sw, "eag", DEFAULT_NAMESPACE, ATTRS, NAMESPACES, () -> {

            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                    DEFAULT_NAMESPACE + " http://schemas.archivesportaleurope.net/profiles/eag.xsd");

            LanguageHelpers.getBestDescription(repository, Optional.empty(), langCode).ifPresent(desc -> {

                addControlSection(sw, repository, country, desc);

                tag(sw, "archguide", () -> {
                    addIdentitySection(sw, repository, desc);

                    tag(sw, ImmutableList.of("desc", "repositories", "repository"), () -> {
                        tag(sw, "geogarea", LanguageHelpers.countryCodeToContinent(country.getCode()).orElse("Europe"));

                        List<Address> addresses = Lists.newArrayList(desc.as(RepositoryDescription.class).getAddresses());
                        if (addresses.isEmpty()) {
                            // NB: A location tag is always needed ¯\_(ツ)_/¯
                            tag(sw, "location", attrs("localType", "postal address"), () -> {
                                tag(sw, "country", LanguageHelpers.countryCodeToName(country.getCode()));
                                tag(sw, "municipalityPostalcode", (String) null);
                                tag(sw, "street", (String) null);
                            });
                        } else {
                            for (Address address : addresses) {
                                tag(sw, "location", attrs("localType", "postal address"), () -> {
                                    String cc = Optional.ofNullable(((String) address.getProperty(ContactInfo.countryCode)))
                                            .orElse(country.getCode());
                                    tag(sw, "country", LanguageHelpers.countryCodeToName(cc));
                                    tag(sw, "municipalityPostalcode", address.<String>getProperty(ContactInfo.postalCode));
                                    tag(sw, "street", address.<String>getProperty(ContactInfo.street));
                                });
                            }
                        }

                        for (ContactInfo contact : new ContactInfo[]{ContactInfo.telephone, ContactInfo.fax}) {
                            for (Address address : (desc.as(RepositoryDescription.class)).getAddresses()) {
                                for (Object value : coerceList(address.getProperty(contact))) {
                                    tag(sw, contact.name(), value.toString());
                                }
                            }
                        }
                        for (ContactInfo ref : new ContactInfo[]{ContactInfo.email, ContactInfo.webpage}) {
                            for (Address address : (desc.as(RepositoryDescription.class)).getAddresses()) {
                                for (Object value : coerceList(address.getProperty(ref))) {
                                    tag(sw, ref.name(), value.toString(), attrs("href", value.toString()));
                                }
                            }
                        }

                        List<String> elems = historyElements
                                .stream().<String>map(desc::getProperty)
                                .filter(v -> v != null).collect(Collectors.toList());
                        if (!elems.isEmpty()) {
                            tag(sw, ImmutableList.of("repositorhist", "descriptiveNote"), () -> {
                                for (String e : elems) {
                                    tag(sw, "p",
                                            attrs("xml:lang", desc.getLanguageOfDescription()), () -> cData(sw, e));
                                }
                            });
                        }

                        addTextElements(sw, desc, Isdiah.buildings, Isdiah.holdings);

                        tag(sw, ImmutableList.of("timetable", "opening"), desc.<String>getProperty(Isdiah.openingTimes));

                        tag(sw, "access", attrs("question", "yes"), () ->
                                Optional.ofNullable(desc.<String>getProperty(Isdiah.conditions)).ifPresent(terms ->
                                        tag(sw, "termsOfUse", terms)
                                )
                        );

                        tag(sw, "accessibility", desc.getProperty(Isdiah.accessibility), attrs("question", "yes"));
                        addTextElements(sw, desc, Isdiah.researchServices);
                    });

                });
            });
        });
    }

    private void addControlSection(XMLStreamWriter sw, Repository repository, Country country, Description desc) {
        tag(sw, "control", () -> {
            tag(sw, "recordId", String.format("%s-%s", country.getCode().toUpperCase(),
                    repository.getIdentifier()));

            tag(sw, "otherRecordId", repository.getId(), attrs("localType", "yes"));

            tag(sw, "maintenanceAgency", () -> {
                tag(sw, "agencyCode", "EHRI");
                tag(sw, "agencyName", "The EHRI Consortium");
            });
            tag(sw, "maintenanceStatus", "revised");

            addRevisionDesc(sw, repository, desc);
        });
    }

    private void addIdentitySection(XMLStreamWriter sw, Repository repository, Description desc) {

        tag(sw, "identity", () -> {
            tag(sw, "repositorid", null,
                    attrs("countrycode", repository.getCountry().getCode().toUpperCase()));
            tag(sw, "autform", desc.getName(), attrs("xml:lang", desc.getLanguageOfDescription()));

            Optional.ofNullable(desc.getProperty(Isdiah.parallelFormsOfName)).ifPresent(parNames -> {
                List values = parNames instanceof List ? (List) parNames : ImmutableList.of(parNames);
                for (Object value : values) {
                    tag(sw, "parform", value.toString());
                }
            });
            Optional.ofNullable(desc.getProperty(Isdiah.otherFormsOfName)).ifPresent(parNames -> {
                List values = parNames instanceof List ? (List) parNames : ImmutableList.of(parNames);
                for (Object value : values) {
                    tag(sw, "parform", value.toString());
                }
            });
        });
    }

    private void addRevisionDesc(XMLStreamWriter sw, Described entity, Description desc) {
        tag(sw, "maintenanceHistory", () -> {

            List<MaintenanceEvent> maintenanceEvents = ImmutableList.copyOf(desc.getMaintenanceEvents());
            for (MaintenanceEvent event : maintenanceEvents) {
                tag(sw, "maintenanceEvent", () -> {
                    tag(sw, "agent", "EHRI");
                    tag(sw, "agentType", MaintenanceEventAgentType.human.name());
                    tag(sw, "eventDateTime", event.<String>getProperty("date"));
                    tag(sw, "eventType", event.getEventType().name());
                });
            }

            List<List<SystemEvent>> systemEvents = ImmutableList.copyOf(api.events().aggregateForItem(entity));
            for (List<SystemEvent> agg : Lists.reverse(systemEvents)) {
                SystemEvent event = agg.get(0);

                tag(sw, "maintenanceEvent", () -> {
                    tag(sw, "agent", Optional.ofNullable(event.getActioner())
                            .map(Actioner::getName).orElse(null));
                    tag(sw, "agentType", MaintenanceEventAgentType.human.name());
                    DateTime dateTime = new DateTime(event.getTimestamp());
                    tag(sw, "eventDateTime", DateTimeFormat.longDateTime().print(dateTime), attrs(
                            "standardDateTime", dateTime.toString()));
                    tag(sw, "eventType", MaintenanceEventType
                            .fromSystemEventType(event.getEventType()).name());
                });
            }

            // We must provide a default event
            if (maintenanceEvents.isEmpty() && systemEvents.isEmpty()) {
                logger.debug("No events found for element {}, using fallback", entity.getId());

                tag(sw, "maintenanceEvent", () -> {
                    tag(sw, "agent", entity.getId());
                    tag(sw, "agentType", MaintenanceEventAgentType.machine.name());
                    DateTime dateTime = DateTime.now();
                    tag(sw, "eventDateTime", DateTimeFormat.longDateTime().print(dateTime), attrs(
                            "standardDateTime", dateTime.toString()));
                    tag(sw, "eventType", MaintenanceEventType.created.name());
                });
            }
        });
    }

    private void addTextElements(XMLStreamWriter sw, Description desc, Isdiah... toAdd) {
        Set<Isdiah> adding = Sets.newHashSet(toAdd);
        final Map<String, String> paraAttrs = attrs("xml:lang", desc.getLanguageOfDescription());
        for (Map.Entry<Isdiah, String> entry : descriptiveTextMappings.entrySet()) {
            if (adding.contains(entry.getKey())) {
                Optional.ofNullable(desc.<String>getProperty(entry.getKey())).ifPresent(prop -> {
                    List<String> tags = Splitter.on("/").splitToList(entry.getValue());
                    tag(sw, tags, () ->
                            tag(sw, ImmutableList.of("descriptiveNote", "p"), paraAttrs, () -> cData(sw, prop)));
                });
            }
        }
    }
}
