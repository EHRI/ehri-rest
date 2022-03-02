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

package eu.ehri.project.exporters.dc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.Isaar;
import eu.ehri.project.definitions.IsadG;
import eu.ehri.project.definitions.Isdiah;
import eu.ehri.project.exporters.xml.AbstractStreamingXmlExporter;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Temporal;
import eu.ehri.project.utils.LanguageHelpers;

import javax.xml.stream.XMLStreamWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


public class DublinCore11Exporter extends AbstractStreamingXmlExporter<Described> implements DublinCoreExporter {

    private static final String OAI_NS = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String DC_NS = "http://purl.org/dc/elements/1.1/";

    private static final Map<String, String> NAMESPACES = namespaces(
            "oai_dc", OAI_NS,
            "xsi", "http://www.w3.org/2001/XMLSchema-instance",
            "dc", DC_NS);

    // Mappings of output tags to internal keys. All values found are accepted.
    private static final Multimap<String, String> propertyMappings = ImmutableMultimap
            .<String, String>builder()
            .putAll("type", ImmutableSet.of(Isaar.typeOfEntity.name(), IsadG.levelOfDescription.name()))
            .putAll("format", ImmutableSet.of(IsadG.extentAndMedium.name()))
            .putAll("language", ImmutableSet.of(IsadG.languageOfMaterial.name()))
            .build();

    private static final Multimap<String, String> textPropertyMappings = ImmutableMultimap
            .<String, String>builder()
            .putAll("description", ImmutableSet.of("abstract",
                    IsadG.scopeAndContent.name(), IsadG.biographicalHistory.name(),
                    Isdiah.history.name(), Isdiah.geoculturalContext.name(),
                    Isaar.generalContext.name()))
            .build();

    // A function to transform values with a given tag
    private static final Map<String, Function<String, String>> valueTransformers = ImmutableMap
            .<String, Function<String, String>>builder()
            .put("language", LanguageHelpers::codeToName)
            .build();


    private final Api api;

    public DublinCore11Exporter(Api api) {
        this.api = api;
    }

    @Override
    public void export(XMLStreamWriter sw, Described item, String langCode) {
        root(sw, "oai_dc:dc", OAI_NS, attrs(), NAMESPACES, () -> {
            attribute(sw, "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation", OAI_NS + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd");

            tag(sw, "dc:identifier", item.getIdentifier());
            Optional<Description> descOpt = LanguageHelpers
                    .getBestDescription(item, Optional.<Description>empty(), langCode);

            descOpt.ifPresent(desc -> {
                String langCode639_1 = LanguageHelpers.iso639DashOneCode(desc.getLanguageOfDescription());

                tag(sw, "dc:title", desc.getName());

                Optional.ofNullable(item.as(DocumentaryUnit.class).getRepository()).ifPresent(repository -> {
                    LanguageHelpers.getBestDescription(repository, langCode).ifPresent(d ->
                            tag(sw, "dc:publisher", d.getName())
                    );
                });

                for (DatePeriod datePeriod : desc.as(Temporal.class).getDatePeriods()) {
                    String start = datePeriod.getStartDate();
                    String end = datePeriod.getEndDate();
                    if (start != null && end != null) {
                        tag(sw, "dc:coverage", String.format("%s - %s", start, end));
                    } else if (start != null) {
                        tag(sw, "dc:coverage", start);
                    }
                }

                for (Map.Entry<String, Collection<String>> attr :
                        propertyMappings.asMap().entrySet()) {
                    for (String key : attr.getValue()) {
                        for (Object value : coerceList(desc.getProperty(key))) {
                            tag(sw, "dc:" + attr.getKey(), transform(attr.getKey(), value));
                        }
                    }
                }

                for (Map.Entry<String, Collection<String>> attr :
                        textPropertyMappings.asMap().entrySet()) {
                    for (String key : attr.getValue()) {
                        for (Object value : coerceList(desc.getProperty(key))) {
                            tag(sw, "dc:" + attr.getKey(),
                                    transform(attr.getKey(), value), attrs("xml:lang", langCode639_1));
                        }
                    }
                }

                for (AccessPoint accessPoint : desc.getAccessPoints()) {
                    AccessPointType type = accessPoint.getRelationshipType();
                    switch (type) {
                        case creator:
                        case subject:
                            tag(sw, "dc:" + type.name(), accessPoint.getName());
                            break;
                        case person:
                        case corporateBody:
                        case family:
                            tag(sw, "dc:relation", accessPoint.getName());
                            break;
                        case place:
                            tag(sw, "dc:coverage", accessPoint.getName());
                            break;
                        default:
                    }
                }
            });
        });
    }

    private String transform(String key, Object value) {
        return valueTransformers.containsKey(key)
                ? valueTransformers.get(key).apply(value.toString())
                : value.toString();
    }
}
