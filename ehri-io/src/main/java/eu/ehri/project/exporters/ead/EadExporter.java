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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.*;
import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.utils.LanguageHelpers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Encoded Archival Description (EAD) export.
 */
public interface EadExporter extends XmlExporter<DocumentaryUnit> {

    Logger logger = LoggerFactory.getLogger(EadExporter.class);

    /**
     * Export a documentary unit as an EAD document.
     *
     * @param unit         the unit
     * @param outputStream the output stream to write to.
     * @param langCode     the preferred language code when multiple
     *                     descriptions are available
     */
    void export(DocumentaryUnit unit,
                OutputStream outputStream, String langCode) throws IOException;

    /**
     * Export a documentary unit as an EAD document.
     *
     * @param unit     the unit
     * @param langCode the preferred language code when multiple
     *                 descriptions are available
     * @return a DOM document
     */
    Document export(DocumentaryUnit unit, String langCode) throws IOException;

    /**
     * Get the EAD tag name corresponding to a given creator access point. This
     * is only available if it links to an Historical Agent instance, where it
     * can be inferred from the typeOfEntity property.
     *
     * @param creatorAccessPoint the access point instance
     * @return an optional tag name
     */
    static Optional<String> getCreatorTagName(AccessPoint creatorAccessPoint, String langCode) {
        for (Link link : creatorAccessPoint.getLinks()) {
            for (Entity target : link.getLinkTargets()) {
                if (target.getType().equals(Entities.HISTORICAL_AGENT)) {
                    HistoricalAgent item = target.as(HistoricalAgent.class);
                    Optional<Description> desc = LanguageHelpers.getBestDescription(item, Optional.empty(), langCode);
                    return desc.flatMap(d -> Optional.ofNullable(d.getProperty(Isaar.typeOfEntity.name())));
                }
            }
        }
        return Optional.empty();
    }

    // Vocabulary-link attributes (source + authority identifier) for an origination tag.
    static Map<String, String> getCreatorAttributes(AccessPoint creatorAccessPoint, String identifierAttr) {
        return getVocabAttributes(creatorAccessPoint, identifierAttr, Entities.HISTORICAL_AGENT);
    }

    // Vocabulary-link attributes (source + authority/concept identifier) for a controlaccess tag.
    static Map<String, String> getAccessPointAttributes(AccessPoint accessPoint, String identifierAttr) {
        return getVocabAttributes(accessPoint, identifierAttr, Entities.CVOC_CONCEPT, Entities.HISTORICAL_AGENT);
    }

    /**
     * @param identifierAttr the attribute name used to hold the authority identifier
     *                       (schema-specific, e.g. "authfilenumber" for EAD2002 or
     *                       "identifier" for EAD3)
     */
    static Map<String, String> getVocabAttributes(AccessPoint accessPoint, String identifierAttr, String... targetTypes) {
        Set<String> types = ImmutableSet.copyOf(targetTypes);
        for (Link link : accessPoint.getLinks()) {
            for (Entity target : link.getLinkTargets()) {
                if (types.contains(target.getType())) {
                    AuthoritativeItem item = target.as(AuthoritativeItem.class);
                    try {
                        return ImmutableMap.of(
                                "source", item.getAuthoritativeSet().getId(),
                                identifierAttr, item.getIdentifier()
                        );
                    } catch (NullPointerException e) {
                        logger.warn("Authoritative item with missing set: {}", item.getId());
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Sort child items by their identifiers.
     *
     * @param api  the data API
     * @param unit the current unit
     * @return an iterable of ordered child items
     */
    static Iterable<DocumentaryUnit> getOrderedChildren(Api api, DocumentaryUnit unit) {
        return api
                .query()
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC)
                .withLimit(-1)
                .withStreaming(true)
                .page(unit.getChildren(), DocumentaryUnit.class);
    }

    /**
     * Get copy link info from a documentary unit
     *
     * @param unit            the documentary unit item
     * @param ignoredLangCode currently unused language parameter
     * @return the descriptive text from any copy links
     */
    static List<String> getCopyInfo(DocumentaryUnit unit, String ignoredLangCode) {
        return StreamSupport.stream(unit.getLinks().spliterator(), false)
                .filter(link ->
                        Objects.equals(link.getLinkType(), LinkType.copy)
                                && Objects.equals(link.getLinkSource(), unit))
                .map(Link::getDescription)
                .filter(d -> Objects.nonNull(d) && !d.trim().isEmpty())
                .collect(Collectors.toList());
    }


    /**
     * Get the EAD attributes for an ISAD(G) text field.
     *
     * @param field the field
     * @param kvs   additional key-value pairs
     * @return an attribute map
     */
    static Map<String, String> textFieldAttrs(IsadG field, String... kvs) {
        Preconditions.checkArgument(kvs.length % 2 == 0);
        Map<String, String> attrs = field.getAnalogueEncoding()
                .map(Collections::singleton)
                .orElse(Collections.emptySet())
                .stream().collect(Collectors.toMap(e -> "encodinganalog", e -> e));
        for (int i = 0; i < kvs.length; i += 2) {
            attrs.put(kvs[i], kvs[i + 1]);
        }
        return attrs;
    }

    /**
     * A language/script pairing for langmaterial export, codes resolved to display names.
     */
    final class LangMaterialEntry {
        final String langCode;
        final String langName;
        final Optional<String> scriptCode;
        final Optional<String> scriptName;

        LangMaterialEntry(String langCode, String langName, Optional<String> scriptCode, Optional<String> scriptName) {
            this.langCode = langCode;
            this.langName = langName;
            this.scriptCode = scriptCode;
            this.scriptName = scriptName;
        }
    }

    /**
     * Pair up languageOfMaterial and scriptOfMaterial values positionally (they're
     * independent lists with no explicit link between entries). A script with no
     * language at the same position is dropped, as neither EAD format can express it.
     */
    static List<LangMaterialEntry> getLangMaterialEntries(List<Object> languages, List<Object> scripts) {
        List<LangMaterialEntry> entries = new ArrayList<>();
        for (int i = 0; i < languages.size(); i++) {
            String langCode = languages.get(i).toString();
            Optional<String> scriptCode = i < scripts.size()
                    ? Optional.of(scripts.get(i).toString())
                    : Optional.empty();
            entries.add(new LangMaterialEntry(
                    langCode,
                    LanguageHelpers.codeToName(langCode),
                    scriptCode,
                    scriptCode.map(LanguageHelpers::scriptCodeToName)));
        }
        return entries;
    }

    /**
     * Get a description's free-text languageOfMaterialNotes value, if set.
     */
    static Optional<String> getLanguageOfMaterialNotes(Description desc) {
        if (!desc.getPropertyKeys().contains(IsadG.languageOfMaterialNotes.name())) {
            return Optional.empty();
        }
        return Optional.ofNullable(desc.<String>getProperty(IsadG.languageOfMaterialNotes));
    }

    /**
     * Get the level-of-description attributes for an archdesc or c-level,
     * on an optional description with an optional level value.
     *
     * @param descOpt      an optional description
     * @param defaultLevel a fallback level value
     * @return an attribute pair, or an empty map
     */
    static Map<String, String> getLevelAttrs(Optional<Description> descOpt, String defaultLevel) {
        String level = descOpt
                .map(d -> d.<String>getProperty(IsadG.levelOfDescription))
                .orElse(defaultLevel);
        return level != null ? ImmutableMap.of("level", level) : Collections.emptyMap();
    }

    /**
     * Format a date for use in an EAD normalised-date attribute (e.g. {@code normal}
     * or {@code standarddate}), truncating it to the precision of the date period.
     * When no precision is set the date is rendered to the day.
     *
     * @param dt         the date to format
     * @param precision  the precision of the date period, possibly null
     * @param hyphenated whether to separate date components with hyphens
     *                   (EAD 3 / ISO 8601) or run them together (EAD 2002)
     * @return a normalised date string
     */
    static String formatNormalDate(DateTime dt, DatePeriod.DatePrecision precision, boolean hyphenated) {
        final String pattern;
        if (precision == DatePeriod.DatePrecision.year) {
            pattern = "YYYY";
        } else if (precision == DatePeriod.DatePrecision.quarter
                || precision == DatePeriod.DatePrecision.month) {
            // ISO 8601 has no quarter form, so it's truncated to month. Unlike
            // day precision below, EAD has no compact YYYYMM form, so always hyphenate.
            pattern = "YYYY-MM";
        } else {
            // week (which ISO 8601 only represents in W-notation), day and
            // unspecified precision all render to the day
            pattern = hyphenated ? "YYYY-MM-dd" : "YYYYMMdd";
        }
        return DateTimeFormat.forPattern(pattern).print(dt);
    }

    /**
     * Get an explicit precision value for date precisions that a truncated ISO 8601
     * date cannot represent, namely {@code quarter} (indistinguishable from a month)
     * and {@code week} (indistinguishable from a day). For year/month/day (and unset)
     * precision the standardised date already conveys the granularity, so this returns
     * null and no marker is needed.
     *
     * @param precision the date period precision, possibly null
     * @return the precision name for quarter/week, otherwise null
     */
    static String localDatePrecision(DatePeriod.DatePrecision precision) {
        return precision == DatePeriod.DatePrecision.quarter
                || precision == DatePeriod.DatePrecision.week
                ? precision.name()
                : null;
    }

    /**
     * Get the resource string corresponding to an event type.
     *
     * @param i18n      the resource bundle
     * @param eventType the event type value
     * @return an i18n string
     */
    static String getEventDescription(ResourceBundle i18n, EventTypes eventType) {
        try {
            return i18n.getString(eventType.name());
        } catch (MissingResourceException e) {
            return eventType.name();
        }
    }
}
