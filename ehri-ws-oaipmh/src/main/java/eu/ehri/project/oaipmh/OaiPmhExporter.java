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

package eu.ehri.project.oaipmh;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.exporters.xml.StreamingXmlDsl;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.oaipmh.errors.OaiPmhError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OaiPmhExporter extends StreamingXmlDsl {

    private static final Logger log = LoggerFactory.getLogger(OaiPmhExporter.class);

    private static final String DEFAULT_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";

    static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter
            .ofPattern("u-MM-dd'T'hh:mm:ss'Z'");

    private static final Map<String, String> NAMESPACES = namespaces(
            "xsi", "http://www.w3.org/2001/XMLSchema-instance"
    );

    private static final Map<String, String> DC_NAMESPACES = namespaces(
            "oai_dc", MetadataPrefix.oai_dc.namespace(),
            "xsi", "http://www.w3.org/2001/XMLSchema-instance",
            "dc", "http://purl.org/dc/elements/1.1/");

    private final OaiPmhRenderer renderer;
    private final Config config;
    private final OaiPmhData data;

    public OaiPmhExporter(OaiPmhData data, OaiPmhRenderer renderer, Config config) {
        this.renderer = renderer;
        this.config = config;
        this.data = data;
    }

    public void performVerb(XMLStreamWriter sw, OaiPmhState state) {
        try {
            switch (state.getVerb()) {
                case Identify:
                    identify(sw, state);
                    break;
                case ListMetadataFormats:
                    listMetadataFormats(sw, state);
                    break;
                case ListSets:
                    listSets(sw, state);
                    break;
                case GetRecord:
                    getRecord(sw, state);
                    break;
                case ListIdentifiers:
                    listIdentifiers(sw, state);
                    break;
                case ListRecords:
                    listRecords(sw, state);
                    break;
            }
        } catch (OaiPmhError e) {
            error(sw, e.getCode(), e.getMessage(), state);
        }
    }

    private void identify(XMLStreamWriter sw, OaiPmhState state) {
        withDoc(sw, () -> {
            preamble(sw, Verb.Identify.name(), state.toMap());
            tag(sw, Verb.Identify.name(), () -> {
                tag(sw, "repositoryName", config.getString("oaipmh.repositoryName"));
                tag(sw, "baseURL", config.getString("oaipmh.baseURL"));
                tag(sw, "protocolVersion", "2.0");
                tag(sw, "adminEmail", config.getString("oaipmh.adminEmail"));
                tag(sw, "earliestDatestamp", formatDate(data.getEarliestTimestamp()));
                tag(sw, "deletedRecord", "persistent");
                tag(sw, "granularity", "YYYY-MM-DDThh:mm:ssZ");
                if (config.hasPath("oaipmh.compression")) {
                    tag(sw, "compression", config.getString("oaipmh.compression"));
                }
            });
        });
    }

    private void listMetadataFormats(XMLStreamWriter sw, OaiPmhState state) throws OaiPmhError {
        if (state.getIdentifier() != null) {
            if (data.getRecord(state).isInvalid()) {
                throw new OaiPmhError(ErrorCode.idDoesNotExist,
                        "Identifier does not exist: " + state.getIdentifier());
            }
        }
        withDoc(sw, () -> {
            preamble(sw, Verb.ListMetadataFormats.name(), state.toMap());
            tag(sw, Verb.ListMetadataFormats.name(), () -> {
                for (MetadataPrefix prefix : MetadataPrefix.values()) {
                    tag(sw, "metadataFormat", () -> {
                        tag(sw, "metadataPrefix", prefix.name());
                        tag(sw, "schema", prefix.schema());
                        tag(sw, "metadataNamespace", prefix.namespace());
                    });
                }
            });
        });
    }

    private void listSets(XMLStreamWriter sw, OaiPmhState state) throws OaiPmhError {
        QueryApi.Page<OaiPmhSet> sets = data.getSets(state);
        long count = sets.getTotal();
        Map<String, String> rtAttrs = getResumptionAttrs(sets);
        withDoc(sw, () -> {
            preamble(sw, Verb.ListSets.name(), state.toMap());
            tag(sw, Verb.ListSets.name(), () -> {
                for (OaiPmhSet set: sets) {
                    tag(sw, "set", () -> {
                        tag(sw, "setSpec", set.getId());
                        tag(sw, "setName", set.getName());
                        tag(sw, "setDescription", () -> dcDescription(sw, set.getDescription()));
                    });
                }
                if (state.shouldResume(Math.toIntExact(count))) {
                    tag(sw, "resumptionToken", state.nextState(), rtAttrs);
                } else if (state.hasResumed()) {
                    tag(sw, "resumptionToken", null, rtAttrs);
                }
            });
        });
    }

    private void getRecord(XMLStreamWriter sw, OaiPmhState state) throws OaiPmhError {
        OaiPmhRecordResult record = data.getRecord(state);
        record.doc().ifPresent(item ->
                withDoc(sw, () -> {
                    preamble(sw, Verb.GetRecord.name(), state.toMap());
                    tag(sw, Verb.GetRecord.name(), () ->
                            tag(sw, "record", () -> {
                                tag(sw, "header", () -> writeRecordHeader(sw, state.getIdentifier(), item));
                                tag(sw, "metadata", () -> renderer.render(sw, state.getMetadataPrefix(), item));
                            }));
                })
        );

        record.deleted().ifPresent(deleted ->
                writeDeletedRecord(sw, deleted.getId(),
                        formatDate(deleted.getDatestamp()), deleted.getSets())
        );

        if (record.isInvalid()) {
            throw new OaiPmhError(ErrorCode.idDoesNotExist,
                    "ID does not exist: " + state.getIdentifier());
        }
    }

    private void listIdentifiers(XMLStreamWriter sw, OaiPmhState state) throws OaiPmhError {
        QueryApi.Page<DocumentaryUnit> items = data.getFilteredDocumentaryUnits(state);
        long count = items.getTotal();
        Iterable<OaiPmhDeleted> deleted = data.getFilteredDeletedDocumentaryUnits(state);
        Map<String, String> rtAttrs = getResumptionAttrs(items);
        if (count == 0 && !deleted.iterator().hasNext()) {
            throw new OaiPmhError(ErrorCode.noRecordsMatch);
        }

        withDoc(sw, () -> {
            preamble(sw, Verb.ListIdentifiers.name(), state.toMap());
            tag(sw, Verb.ListIdentifiers.name(), () -> {
                for (DocumentaryUnit item : items) {
                    tag(sw, "header", () -> writeRecordHeader(sw, item.getId(), item));
                }
                if (state.shouldResume(Math.toIntExact(count))) {
                    tag(sw, "resumptionToken", state.nextState(), rtAttrs);
                } else {
                    for (OaiPmhDeleted item : deleted) {
                        writeDeletedRecord(sw, item.getId(), formatDate(item.getDatestamp()), item.getSets());
                    }
                    if (state.hasResumed()) {
                        tag(sw, "resumptionToken", null, rtAttrs);
                    }
                }
            });
        });
    }

    private void listRecords(XMLStreamWriter sw, OaiPmhState state) throws OaiPmhError {
        LocalDateTime before = LocalDateTime.now();
        QueryApi.Page<DocumentaryUnit> items = data.getFilteredDocumentaryUnits(state);
        long count = items.getTotal();
        Map<String, String> rtAttrs = getResumptionAttrs(items);
        Iterable<OaiPmhDeleted> deleted = data.getFilteredDeletedDocumentaryUnits(state);
        if (count == 0 && !deleted.iterator().hasNext()) {
            throw new OaiPmhError(ErrorCode.noRecordsMatch);
        }
        LocalDateTime after = LocalDateTime.now();
        log.debug("Fetched {} items in {} millis", count, before.until(after, ChronoUnit.MILLIS));

        withDoc(sw, () -> {
            preamble(sw, Verb.ListRecords.name(), state.toMap());
            tag(sw, Verb.ListRecords.name(), () -> {
                for (DocumentaryUnit item : items) {
                    tag(sw, "record", () -> {
                        tag(sw, "header", () -> writeRecordHeader(sw, item.getId(), item));
                        tag(sw, "metadata", () -> renderer.render(sw, state.getMetadataPrefix(), item));
                    });
                }
                if (state.shouldResume(Math.toIntExact(count))) {
                    tag(sw, "resumptionToken", state.nextState(), rtAttrs);
                } else {
                    for (OaiPmhDeleted item : deleted) {
                        tag(sw, "record", () ->
                                writeDeletedRecord(sw, item.getId(), formatDate(item.getDatestamp()), item.getSets()));
                    }
                    if (state.hasResumed()) {
                        tag(sw, "resumptionToken", null, rtAttrs);
                    }
                }
            });
        });
    }

    private void writeRecordHeader(XMLStreamWriter sw, String id, DocumentaryUnit item) {
        tag(sw, "identifier", id);
        SystemEvent event = item.getLatestEvent();
        tag(sw, "datestamp", event != null
                ? formatDate(event.getTimestamp())
                : formatDate(ZonedDateTime.now()));
        tag(sw, "setSpec", item.getRepository().getCountry().getCode());
        tag(sw, "setSpec", item.getRepository().getCountry().getCode() + ":"
                + item.getRepository().getId());
    }

    private void writeDeletedRecord(XMLStreamWriter sw, String id, String timestamp, List<String> sets) {
        tag(sw, "header", attrs("status", "deleted"), () -> {
            tag(sw, "identifier", id);
            tag(sw, "datestamp", timestamp);
            for (String setSpec : sets) {
                tag(sw, "setSpec", setSpec);
            }
        });
    }

    private void withDoc(XMLStreamWriter sw, Runnable runnable) {
        doc(sw, () -> root(sw, "OAI-PMH", DEFAULT_NAMESPACE, attrs(), NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", DEFAULT_NAMESPACE + " http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd " +
                            Joiner.on(' ').join(ImmutableList.of(
                                    MetadataPrefix.oai_dc.namespace(), MetadataPrefix.oai_dc.schema(),
                                    MetadataPrefix.ead.namespace(), MetadataPrefix.ead.schema()
                            ))
            );
            runnable.run();
        }));
    }

    private void dcDescription(XMLStreamWriter sw, String description) {
        root(sw, "oai_dc:dc", null, attrs(), DC_NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", MetadataPrefix.oai_dc.namespace()
                            + " " + MetadataPrefix.oai_dc.schema());
            tag(sw, "dc:description", description);
        });
    }

    private void preamble(XMLStreamWriter sw, String verb, Map<String, String> attrs) {
        String time = formatDate(ZonedDateTime.now());
        HashMap<String, String> attrMap = Maps.newHashMap();
        attrMap.putAll(attrs);
        if (verb != null) {
            attrMap.put("verb", verb);
        }
        tag(sw, "responseDate", time);
        tag(sw, "request", config.getString("oaipmh.baseURL"), attrMap);
    }

    private void error(XMLStreamWriter sw, ErrorCode code, String msg, OaiPmhState state) {
        Map<String, String> attrs = attrs("metadataPrefix", state.getMetadataPrefix());
        if (state.getIdentifier() != null) {
            attrs.put("identifier", state.getIdentifier());
        }
        withDoc(sw, () -> {
            preamble(sw, state.getVerb().name(), attrs);
            tag(sw, "error", msg, attrs("code", code.name()));
        });
    }

    public void error(XMLStreamWriter sw, ErrorCode code, String msg, Verb verb) {
        withDoc(sw, () -> {
            preamble(sw, verb != null ? verb.name() : null, attrs());
            tag(sw, "error", msg, attrs("code", code.name()));
        });
    }

    private Map<String, String> getResumptionAttrs(QueryApi.Page<?> page) {
        return attrs("completeListSize", page.getTotal(), "cursor", page.getOffset());
    }

    private static String formatDate(String timestamp) {
        return formatDate(ZonedDateTime.parse(timestamp));
    }

    private static String formatDate(ZonedDateTime time) {
        return time.format(DATE_PATTERN);
    }
}
