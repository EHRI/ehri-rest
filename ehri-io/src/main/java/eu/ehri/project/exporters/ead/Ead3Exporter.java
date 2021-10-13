package eu.ehri.project.exporters.ead;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.*;
import eu.ehri.project.exporters.xml.AbstractStreamingXmlExporter;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.utils.LanguageHelpers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class Ead3Exporter extends AbstractStreamingXmlExporter<DocumentaryUnit> implements EadExporter {

    private static final Logger logger = LoggerFactory.getLogger(Ead3Exporter.class);
    private static final Config config = ConfigFactory.load();
    private static final DateTimeFormatter unitDateNormalFormat = DateTimeFormat.forPattern("YYYY-MM-dd");

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

    private final Api api;

    public Ead3Exporter(Api api) {
        this.api = api;
    }

    @Override
    public void export(XMLStreamWriter sw, DocumentaryUnit unit, String langCode) {

        root(sw, "ead", DEFAULT_NAMESPACE, attrs(), NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", DEFAULT_NAMESPACE);

            Repository repository = unit.getRepository();
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
                    descOpt.ifPresent(desc -> addFileDesc(sw, langCode, repository, desc));
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
                    addDataSection(sw, repository, unit, desc, langCode);
                    addPropertyValues(sw, unit, desc, langCode);
                    Iterable<DocumentaryUnit> orderedChildren = getOrderedChildren(unit);
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
                    String eventDesc = getEventDescription(event.getEventType());
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

    private void addDataSection(XMLStreamWriter sw, Repository repository, DocumentaryUnit subUnit,
                                Description desc, String langCode) {
        tag(sw, "did", () -> {
            tag(sw, "unitid", subUnit.getIdentifier());
            tag(sw, "unittitle", desc.getName(), attrs("encodinganalog", "3.1.2"));

            // Render structured dates. Additional unstructured dates are possible in <unitdate>
            for (DatePeriod datePeriod : desc.as(DocumentaryUnitDescription.class).getDatePeriods()) {
                String start = datePeriod.getStartDate();
                String end = datePeriod.getEndDate();
                if (start != null && end != null) {
                    DateTime startDateTime = DateTime.parse(start);
                    DateTime endDateTime = DateTime.parse(end);
                    tag(sw, "unitdatestructured", attrs("encodinganalog", "3.1.3"), () -> {
                        tag(sw, "daterange", () -> {
                            tag(sw, "fromdate", Integer.toString(startDateTime.year().get()),
                                    attrs("standarddate", unitDateNormalFormat.print(startDateTime)));
                           tag(sw, "todate", Integer.toString(endDateTime.year().get()),
                                   attrs("standarddate", unitDateNormalFormat.print(endDateTime)));
                        });
                    });
                } else if (start != null || end != null) {
                    String date = start != null ? start : end;
                    DateTime dt = DateTime.parse(date);
                    String stdDate = unitDateNormalFormat.print(dt);
                    String text = String.format("%s", dt.year().get());
                    tag(sw, "unitdatestructured", attrs("encodinganalog", "3.1.3"), () -> {
                        tag(sw, "datesingle", text, attrs("standarddate", stdDate));
                    });
                }
            }

            Set<String> propertyKeys = desc.getPropertyKeys();
            for (Map.Entry<IsadG, String> pair : textDidMappings.entrySet()) {
                if (propertyKeys.contains(pair.getKey().name())) {
                    for (Object v : coerceList(desc.getProperty(pair.getKey()))) {
                        tag(sw, pair.getValue(), v.toString(), textFieldAttrs(pair.getKey()));
                    }
                }
            }

            if (propertyKeys.contains(IsadG.languageOfMaterial.name())) {
                tag(sw, "langmaterial", () -> {
                    for (Object v : coerceList(desc.getProperty(IsadG.languageOfMaterial))) {
                        String langName = LanguageHelpers.codeToName(v.toString());
                        if (v.toString().length() != 3) {
                            tag(sw, "language", langName, textFieldAttrs(IsadG.languageOfMaterial));
                        } else {
                            tag(sw, "language", langName, textFieldAttrs(IsadG.languageOfMaterial, "langcode", v
                                    .toString()));
                        }
                    }
                });
            }

            Optional.ofNullable(repository).ifPresent(repo -> {
                LanguageHelpers.getBestDescription(repo, Optional.empty(), langCode).ifPresent(repoDesc ->
                    tag(sw, path("repository", "corpname", "part"), repoDesc.getName())
                );
            });
        });
    }

    private void addEadLevel(XMLStreamWriter sw, int num, DocumentaryUnit subUnit,
                             Optional<Description> priorDescOpt, String langCode) {
        logger.trace("Adding EAD sublevel: c{}", num);
        Optional<Description> descOpt = LanguageHelpers.getBestDescription(subUnit, priorDescOpt, langCode);
        String levelTag = String.format("c%02d", num);
        tag(sw, levelTag, getLevelAttrs(descOpt, null), () -> {
            descOpt.ifPresent(desc -> {
                addDataSection(sw, null, subUnit, desc, langCode);
                addPropertyValues(sw, subUnit, desc, langCode);
                addControlAccess(sw, desc);
            });

            for (DocumentaryUnit child : getOrderedChildren(subUnit)) {
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
                List<String> copyInfo = getCopyInfo(unit, langCode);
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

    private Map<String, String> textFieldAttrs(IsadG field, String... kvs) {
        Preconditions.checkArgument(kvs.length % 2 == 0);
        Map<String, String> attrs = field.getAnalogueEncoding()
                .map(Collections::singleton)
                .orElse(Collections.emptySet())
                .stream().collect(Collectors.toMap(e -> "encodinganalog", e -> e));
        for (int i = 0; i < kvs.length; i += 2) {
            attrs.put(kvs[0], kvs[i + 1]);
        }
        return attrs;
    }

    private Map<String, String> getLevelAttrs(Optional<Description> descOpt, String defaultLevel) {
        String level = descOpt
                .map(d -> d.<String>getProperty(IsadG.levelOfDescription))
                .orElse(defaultLevel);
        return level != null ? ImmutableMap.of("level", level) : Collections.emptyMap();
    }

    // Sort the children by identifier. FIXME: This might be a bad assumption!
    private Iterable<DocumentaryUnit> getOrderedChildren(DocumentaryUnit unit) {
        return api
                .query()
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC)
                .withLimit(-1)
                .withStreaming(true)
                .page(unit.getChildren(), DocumentaryUnit.class);
    }

    private List<String> getCopyInfo(DocumentaryUnit unit, String langCode) {
        return StreamSupport.stream(unit.getLinks().spliterator(), false)
                .filter(link ->
                        Objects.equals(link.getLinkType(), "copy")
                                && Objects.equals(link.getLinkSource(), unit))
                .map(Link::getDescription)
                .filter(d -> Objects.nonNull(d) && !d.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String getEventDescription(EventTypes eventType) {
        try {
            return i18n.getString(eventType.name());
        } catch (MissingResourceException e) {
            return eventType.name();
        }
    }
}
