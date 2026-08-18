/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.exporters.ead;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.ContactInfo;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.IsadG;
import eu.ehri.project.exporters.xml.AbstractStreamingXmlExporter;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Named;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.utils.LanguageHelpers;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import static eu.ehri.project.exporters.ead.EadExporter.textFieldAttrs;
import static eu.ehri.project.exporters.ead.EadExporter.getLevelAttrs;
import static eu.ehri.project.exporters.ead.EadExporter.getEventDescription;
import static eu.ehri.project.exporters.ead.EadExporter.getLangMaterialEntries;
import static eu.ehri.project.exporters.ead.EadExporter.getLanguageOfMaterialNotes;
import eu.ehri.project.exporters.ead.EadExporter.LangMaterialEntry;
import static eu.ehri.project.exporters.ead.EadExporter.formatNormalDate;
import static eu.ehri.project.exporters.ead.EadExporter.localDatePrecision;


public class Ead3Exporter extends AbstractStreamingXmlExporter<DocumentaryUnit> implements EadExporter {

    private static final Logger logger = LoggerFactory.getLogger(Ead3Exporter.class);

    private static final ResourceBundle i18n = ResourceBundle.getBundle(Ead3Exporter.class.getName());

    private static final String DEFAULT_NAMESPACE = "http://ead3.archivists.org/schema/";
    private static final Map<String, String> NAMESPACES = namespaces(
            "xlink", "http://www.w3.org/1999/xlink",
            "xsi", "http://www.w3.org/2001/XMLSchema-instance"
    );

    private static final Map<IsadG, String> multiValueTextMappings = ImmutableMap.<IsadG, String>builder()
            .put(IsadG.archivistNote, "processinfo")
            .put(IsadG.scopeAndContent, "scopecontent")
            .put(IsadG.systemOfArrangement, "arrangement")
            .put(IsadG.publicationNote, "bibliography")
            .put(IsadG.locationOfCopies, "altformavail")
            .put(IsadG.locationOfOriginals, "originalsloc")
            .put(IsadG.biographicalHistory, "bioghist")
            .put(IsadG.conditionsOfAccess, "accessrestrict")
            .put(IsadG.conditionsOfReproduction, "userestrict")
            .put(IsadG.findingAids, "otherfindaid")
            .put(IsadG.accruals, "accruals")
            .put(IsadG.acquisition, "acqinfo")
            .put(IsadG.appraisal, "appraisal")
            .put(IsadG.archivalHistory, "custodhist")
            .put(IsadG.physicalCharacteristics, "phystech")
            .put(IsadG.relatedUnitsOfDescription, "relatedmaterial")
            .put(IsadG.separatedUnitsOfDescription, "separatedmaterial")
            .put(IsadG.notes, "odd") // controversial!
            .build();

    private static final Map<IsadG, String> textDidMappings = ImmutableMap.<IsadG, String>builder()
            .put(IsadG.extentAndMedium, "physdesc")
            .put(IsadG.unitDates, "unitdate")
            .build();

    private static final Map<AccessPointType, String> controlAccessMappings = ImmutableMap.<AccessPointType, String>builder()
            .put(AccessPointType.subject, "subject")
            .put(AccessPointType.person, "persname")
            .put(AccessPointType.family, "famname")
            .put(AccessPointType.corporateBody, "corpname")
            .put(AccessPointType.place, "geogname")
            .put(AccessPointType.genre, "genreform")
            .build();

    private static final List<ContactInfo> addressKeys = ImmutableList
            .of(ContactInfo.street,
                    ContactInfo.postalCode,
                    ContactInfo.municipality,
                    ContactInfo.firstdem,
                    ContactInfo.countryCode,
                    ContactInfo.telephone,
                    ContactInfo.fax,
                    ContactInfo.webpage,
                    ContactInfo.email);

    private static final Map<String, String> creatorTags = ImmutableMap.<String, String>builder()
            .put("corporateBody", "corpname")
            .put("family", "famname")
            .put("person", "persname")
            .build();


    private final Api api;

    public Ead3Exporter(Api api) {
        this.api = api;
    }

    @Override
    public void export(XMLStreamWriter sw, DocumentaryUnit unit, String langCode) {

        root(sw, "ead", DEFAULT_NAMESPACE, attrs(), NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", DEFAULT_NAMESPACE);

            Optional<Repository> repoOpt = Optional.ofNullable(unit.getRepository());
            Optional<Description> descOpt = LanguageHelpers.getBestDescription(unit, Optional.empty(), langCode);
            String title = descOpt.map(Description::getName).orElse(unit.getIdentifier());

            tag(sw, "control", attrs("relatedencoding", "DC",
                    "scriptencoding", "iso15924",
                    "repositoryencoding", "iso15511",
                    "dateencoding", "iso8601",
                    "countryencoding", "iso3166-1"), () -> {

                tag(sw, "recordid", unit.getId());
                tag(sw, "filedesc", () -> {
                    tag(sw, "titlestmt", () -> tag(sw, "titleproper", title));
                    descOpt.ifPresent(desc -> {
                        repoOpt.ifPresent( repo -> {
                                addFileDesc(sw, langCode, repo, desc);
                        });
                    });
                });

                tag(sw, "maintenancestatus", attrs("value", "derived"));
                tag(sw, path("maintenanceagency", "agencyname"), "EHRI");
                tag(sw, "languagedeclaration", () -> {
                    tag(sw, "language", LanguageHelpers.codeToName(langCode), attrs("langcode", langCode));

                    // FIXME: not sure we have this info?...
                    comment(sw, "Beware: this (assumed) script code may be inaccurate...");
                    tag(sw, "script", "Latin", attrs("scriptcode", "latn"));
                });

                descOpt.flatMap(desc -> Optional.ofNullable(desc.<String>getProperty(IsadG.rulesAndConventions))).ifPresent(value -> {
                    tag(sw, "conventiondeclaration", () -> {
                        tag(sw, "citation", () -> {});
                        tag(sw, "descriptivenote", attrs("encodinganalog", "3.7.2"), () -> {
                            tag(sw, "p", value);
                        });
                    });
                });

                addRevisionDesc(sw, unit);
            });

            descOpt.ifPresent(desc -> {
                tag(sw, "archdesc", getLevelAttrs(descOpt, "collection"), () -> {
                    addDataSection(sw, unit, desc, langCode, repoOpt);
                    addPropertyValues(sw, unit, desc, langCode);
                    Iterable<DocumentaryUnit> orderedChildren = EadExporter.getOrderedChildren(api, unit);
                    if (orderedChildren.iterator().hasNext()) {
                        tag(sw, "dsc", () -> {
                            for (DocumentaryUnit child : orderedChildren) {
                                addEadLevel(sw, 1, child, descOpt, langCode);
                            }
                        });
                    }
                    addControlAccess(sw, desc);
                });
            });
        });
    }

    private void addFileDesc(XMLStreamWriter sw, String langCode, Repository repository, Description desc) {
        tag(sw, "publicationstmt", () -> {
            LanguageHelpers.getBestDescription(repository, Optional.empty(), langCode).ifPresent(repoDesc -> {
                tag(sw, "publisher", repoDesc.getName());
                for (Address address : repoDesc.as(RepositoryDescription.class).getAddresses()) {
                    tag(sw, "address", () -> {
                        for (ContactInfo key : addressKeys) {
                            for (Object v : coerceList(address.getProperty(key))) {
                                tag(sw, "addressline", v.toString());
                            }
                        }
                        tag(sw, "addressline",
                                LanguageHelpers.countryCodeToName(
                                        repository.getCountry().getId()));
                    });
                }
            });
        });
        if (Description.CreationProcess.IMPORT.equals(desc.getCreationProcess())) {
            tag(sw, ImmutableList.of("notestmt", "controlnote", "p"), resourceAsString("creationprocess-boilerplate.txt"));
        }
    }

    private void addRevisionDesc(XMLStreamWriter sw, DocumentaryUnit unit) {
        tag(sw, "maintenancehistory", () -> {
            tag(sw, "maintenanceevent", () -> {
                tag(sw, "eventtype", attrs("value", "derived"));
                tag(sw, "eventdatetime", DateTime.now().toString());
                tag(sw, "agenttype", attrs("value", "machine"));
                tag(sw, "agent", "EHRI Portal");
                tag(sw, "eventdescription", resourceAsString("export-boilerplate.txt"));
            });
            List<List<SystemEvent>> eventList = Lists.newArrayList(api.events().aggregateForItem(unit));
            if (!eventList.isEmpty()) {
                for (List<SystemEvent> agg : eventList) {
                    SystemEvent event = agg.get(0);
                    String eventDesc = getEventDescription(i18n, event.getEventType());
                    String text = event.getLogMessage() == null || event.getLogMessage().trim().isEmpty()
                            ? eventDesc
                            : String.format("%s [%s]", event.getLogMessage(), eventDesc);
                    tag(sw, "maintenanceevent", () -> {
                        tag(sw, "eventtype", attrs("value", "derived"));
                        tag(sw, "eventdatetime", new DateTime(event.getTimestamp()).toString());
                        tag(sw, "agenttype", attrs("value", "machine"));
                        tag(sw, "agent", "EHRI Portal");
                        tag(sw, "eventdescription", text);
                    });
                }
            }
        });
    }

    private void addDataSection(XMLStreamWriter sw, DocumentaryUnit subUnit, Description desc, String langCode, Optional<Repository> repoOpt) {
        tag(sw, "did", () -> {
            tag(sw, "unitid", subUnit.getIdentifier());
            tag(sw, "unitid",
                    String.format("%s%s", config.getString("io.pids.prefix"), Objects.toString(subUnit.getPid(), "")),
                    attrs("label", config.getString("io.pids.label"), "localtype", "ark"));
            tag(sw, "unittitle", desc.getName(), attrs("encodinganalog", "3.1.2"));
            addOrigination(sw, desc, langCode);
            addDatePeriods(sw, desc);
            addDidProperties(sw, desc);
            repoOpt.ifPresent(repo -> {
                addRepositoryInfo(sw, langCode, repo);
            });
        });
    }

    private void addOrigination(XMLStreamWriter sw, Description desc, String langCode) {
        List<AccessPoint> creators = StreamSupport.stream(desc.getAccessPoints().spliterator(), false)
                .filter(ap -> ap.getRelationshipType().equals(AccessPointType.creator))
                .sorted(Comparator.comparing(Named::getName))
                .collect(Collectors.toList());
        if (!creators.isEmpty()) {
            for (AccessPoint creatorAccessPoint : creators) {
                tag(sw, "origination", attrs("label", "creator"), () -> {
                    String name = creatorAccessPoint.getName();
                    String tagName = creatorTags.get(EadExporter.getCreatorTagName(creatorAccessPoint, langCode).orElse("person"));
                    Map<String, String> attrs = EadExporter.getCreatorAttributes(creatorAccessPoint);
                    tag(sw, tagName, attrs, () -> {
                        tag(sw, "part", name);
                    });
                });
            }
        }
    }

    private void addRepositoryInfo(XMLStreamWriter sw, String langCode, Repository repo) {
        LanguageHelpers.getBestDescription(repo, Optional.empty(), langCode)
                .ifPresent(repoDesc ->
                    tag(sw, path("repository", "corpname", "part"), repoDesc.getName()));
    }

    private void addDidProperties(XMLStreamWriter sw, Description desc) {
        Set<String> propertyKeys = desc.getPropertyKeys();
        for (Map.Entry<IsadG, String> pair : textDidMappings.entrySet()) {
            if (propertyKeys.contains(pair.getKey().name())) {
                for (Object v : coerceList(desc.getProperty(pair.getKey()))) {
                    tag(sw, pair.getValue(), v.toString(), textFieldAttrs(pair.getKey()));
                }
            }
        }

        List<Object> languages = coerceList(desc.getProperty(IsadG.languageOfMaterial));
        List<Object> scripts = coerceList(desc.getProperty(IsadG.scriptOfMaterial));
        Optional<String> notes = getLanguageOfMaterialNotes(desc);
        if (!languages.isEmpty() || notes.isPresent()) {
            tag(sw, "langmaterial", () -> {
                for (LangMaterialEntry entry : getLangMaterialEntries(languages, scripts)) {
                    if (entry.scriptCode.isPresent()) {
                        String scriptCode = entry.scriptCode.get();
                        tag(sw, "languageset", () -> {
                            tag(sw, "language", entry.langName, textFieldAttrs(IsadG.languageOfMaterial,
                                    "langcode", entry.langCode));
                            tag(sw, "script", entry.scriptName.orElse(scriptCode),
                                    textFieldAttrs(IsadG.scriptOfMaterial, "scriptcode", scriptCode));
                        });
                    } else {
                        tag(sw, "language", entry.langName, textFieldAttrs(IsadG.languageOfMaterial,
                                "langcode", entry.langCode));
                    }
                }
                notes.ifPresent(text -> tag(sw, "descriptivenote", () -> tag(sw, "p", text)));
            });
        }
    }

    private void addDatePeriods(XMLStreamWriter sw, Description desc) {
        // Render structured dates. Additional unstructured dates are possible in <unitdate>
        for (DatePeriod datePeriod : desc.as(DocumentaryUnitDescription.class).getDatePeriods()) {
            String start = datePeriod.getStartDate();
            String end = datePeriod.getEndDate();
            DatePeriod.DatePrecision precision = datePeriod.getPrecision();
            // The truncated standarddate expresses year/month/day precision natively, but
            // ISO 8601 cannot distinguish a quarter from a month, or a week from a day, so
            // for those we retain the precision explicitly in the local-semantics @localtype.
            String localType = localDatePrecision(precision);
            DateTime startDateTime = start != null ? DateTime.parse(start) : null;
            DateTime endDateTime = end != null ? DateTime.parse(end) : null;
            String startStd = startDateTime != null ? formatNormalDate(startDateTime, precision, true) : null;
            String endStd = endDateTime != null ? formatNormalDate(endDateTime, precision, true) : null;

            // Skip the range if truncation makes start and end coincide.
            if (startDateTime != null && endDateTime != null && !startStd.equals(endStd)) {
                tag(sw, "unitdatestructured", attrs("encodinganalog", "3.1.3"), () -> {
                    tag(sw, "daterange", () -> {
                        tag(sw, "fromdate", Integer.toString(startDateTime.year().get()),
                                attrs("standarddate", startStd, "localtype", localType));
                       tag(sw, "todate", Integer.toString(endDateTime.year().get()),
                               attrs("standarddate", endStd, "localtype", localType));
                    });
                });
            } else if (startDateTime != null || endDateTime != null) {
                DateTime dt = startDateTime != null ? startDateTime : endDateTime;
                String stdDate = startDateTime != null ? startStd : endStd;
                String text = Integer.toString(dt.year().get());
                tag(sw, "unitdatestructured", attrs("encodinganalog", "3.1.3"), () -> {
                    tag(sw, "datesingle", text, attrs("standarddate", stdDate, "localtype", localType));
                });
            }
        }
    }

    private void addEadLevel(XMLStreamWriter sw, int num, DocumentaryUnit subUnit,
                             Optional<Description> priorDescOpt, String langCode) {
        logger.trace("Adding EAD sublevel: c{}", num);
        Optional<Description> descOpt = LanguageHelpers.getBestDescription(subUnit, priorDescOpt, langCode);
        String levelTag = String.format("c%02d", num);
        tag(sw, levelTag, getLevelAttrs(descOpt, null), () -> {
            descOpt.ifPresent(desc -> {
                addDataSection(sw, subUnit, desc, langCode, Optional.empty());
                addPropertyValues(sw, subUnit, desc, langCode);
                addControlAccess(sw, desc);
            });

            for (DocumentaryUnit child : EadExporter.getOrderedChildren(api, subUnit)) {
                addEadLevel(sw, num + 1, child, descOpt, langCode);
            }
        });
    }

    private void addControlAccess(XMLStreamWriter sw, Description desc) {
        Map<AccessPointType, List<AccessPoint>> byType = Maps.newHashMap();
        for (AccessPoint accessPoint : desc.getAccessPoints()) {
            AccessPointType type = accessPoint.getRelationshipType();
            if (controlAccessMappings.containsKey(type)) {
                if (byType.containsKey(type)) {
                    byType.get(type).add(accessPoint);
                } else {
                    byType.put(type, Lists.newArrayList(accessPoint));
                }
            }
        }

        for (Map.Entry<AccessPointType, List<AccessPoint>> entry : byType.entrySet()) {
            tag(sw, "controlaccess", () -> {
                AccessPointType type = entry.getKey();
                for (AccessPoint accessPoint : entry.getValue()) {
                    tag(sw, controlAccessMappings.get(type), getAccessPointAttributes(accessPoint), () -> {
                       tag(sw, "part", accessPoint.getName());
                    });
                }
            });
        }
    }

    private Map<String, String> getAccessPointAttributes(AccessPoint accessPoint) {
        for (Link link : accessPoint.getLinks()) {
            for (Entity target : link.getLinkTargets()) {
                if (target.getType().equals(Entities.CVOC_CONCEPT) ||
                        target.getType().equals(Entities.HISTORICAL_AGENT)) {
                    AuthoritativeItem item = target.as(AuthoritativeItem.class);
                    try {
                        return ImmutableMap.of(
                                "source", item.getAuthoritativeSet().getId(),
                                "identifier", item.getIdentifier()
                        );
                    } catch (NullPointerException e) {
                        logger.warn("Authoritative item with missing set: {}", item.getId());
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    private void addPropertyValues(XMLStreamWriter sw, DocumentaryUnit unit, Entity item, String langCode) {
        Set<String> available = item.getPropertyKeys();
        for (Map.Entry<IsadG, String> pair : multiValueTextMappings.entrySet()) {
            if (available.contains(pair.getKey().name())) {
                for (Object v : coerceList(item.getProperty(pair.getKey()))) {
                    tag(sw, pair.getValue(), textFieldAttrs(pair.getKey()), () ->
                            tag(sw, "p", () -> cData(sw, v.toString()))
                    );
                }
            }
            if (pair.getKey().equals(IsadG.locationOfOriginals)) {
                List<String> copyInfo = EadExporter.getCopyInfo(unit, langCode);
                if (!copyInfo.isEmpty()) {
                    tag(sw, pair.getValue(), () -> {
                        for (String note : copyInfo) {
                            tag(sw, "p", () -> cData(sw, note));
                        }
                    });
                }
            }
        }
        for (Object v : coerceList(item.getProperty(IsadG.datesOfDescriptions))) {
            tag(sw, "processinfo", textFieldAttrs(IsadG.datesOfDescriptions), () -> {
                tag(sw, path("p", "date"), () -> cData(sw, v.toString()));
            });
        }
        if (available.contains(IsadG.sources.name())) {
            tag(sw, "processinfo", textFieldAttrs(IsadG.sources, "localtype", "Sources"), () -> {
                tag(sw, "p", () -> {
                    for (Object v : coerceList(item.getProperty(IsadG.sources))) {
                        tag(sw, "ref", () -> cData(sw, v.toString()));
                    }
                });
            });
        }
    }
}
