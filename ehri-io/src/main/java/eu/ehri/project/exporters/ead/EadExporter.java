package eu.ehri.project.exporters.ead;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.*;
import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.utils.LanguageHelpers;
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

    /**
     * Get any attributes to the origination/persname (or corpname, famname) tag,
     * specifically the source of the vocabulary and the authority file ID.
     *
     * @param creatorAccessPoint an access point instance
     * @return an attribute map, possibly empty
     */
    static Map<String, String> getCreatorAttributes(AccessPoint creatorAccessPoint) {
        for (Link link : creatorAccessPoint.getLinks()) {
            for (Entity target : link.getLinkTargets()) {
                if (target.getType().equals(Entities.HISTORICAL_AGENT)) {
                    HistoricalAgent item = target.as(HistoricalAgent.class);
                    try {
                        return ImmutableMap.of(
                                "source", item.getAuthoritativeSet().getId(),
                                "authfilenumber", item.getIdentifier()
                        );
                    } catch (NullPointerException e) {
                        logger.warn("HistoricalAgent creator item with missing set: {}", item.getId());
                    }
                }
            }
        }
        return Maps.newHashMap();
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
            attrs.put(kvs[0], kvs[i + 1]);
        }
        return attrs;
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
