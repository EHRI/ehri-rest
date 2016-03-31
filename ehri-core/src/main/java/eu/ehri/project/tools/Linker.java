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

package eu.ehri.project.tools;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Slugify;
import eu.ehri.project.views.impl.CrudViews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Utility class for performing operations on the database
 * related to the creation and validation of links between
 * items.
 */
public class Linker {

    private static final Logger logger = LoggerFactory.getLogger(Linker.class);

    private static final String LINK_TYPE = "associative";
    private static final String DEFAULT_LANG = "eng";

    private final FramedGraph<?> graph;
    private final boolean tolerant;
    private final boolean excludeSingles;
    private final Set<String> accessPointTypes;
    private final String defaultLanguageCode;
    private final Optional<String> logMessage;

    private Linker(FramedGraph<?> graph, Set<String> accessPointTypes,
            String defaultLanguageCode, Optional<String> logMessage,
            boolean tolerant, boolean excludeSingles) {
        this.graph = graph;
        this.accessPointTypes = accessPointTypes;
        this.defaultLanguageCode = defaultLanguageCode;
        this.tolerant = tolerant;
        this.excludeSingles = excludeSingles;
        this.logMessage = logMessage;
    }

    public Linker(FramedGraph<?> graph) {
        this(graph, Sets.<String>newHashSet(),
                DEFAULT_LANG, Optional.<String>absent(), false, true);
    }

    /**
     * Populate a pre-created vocabulary with concepts created based on
     * access points for all collections within a repository, then link
     * those concepts to the relevant documentary units.
     * <p>
     * One creation event will be generated for the newly-created concepts
     * (with the vocabulary as the scope) and another for the newly-created
     * links. Currently events will still be created if no concepts/links
     * are made.
     * <p>
     * It should be advised that this function is not idempotent and
     * running it twice will generate concepts/links twice.
     * <p>
     * NB. One could argue this function does too much...
     *
     * @param repository       the repository
     * @param vocabulary       an existing (presumably empty) vocabulary
     * @param user             the user to whom to attribute the operation
     * @return the number of new links created
     * @throws ValidationError
     * @throws PermissionDenied
     */
    public long createAndLinkRepositoryVocabulary(
            Repository repository,
            Vocabulary vocabulary,
            UserProfile user)
            throws ValidationError, PermissionDenied {

        // First, build a map of access point names to (null) concepts
        Map<String, String> conceptIdentifierNames = Maps.newHashMap();
        Map<String, Optional<Concept>> identifierConcept = Maps.newHashMap();
        Map<String, Integer> identifierCount = Maps.newHashMap();

        for (DocumentaryUnit doc : repository.getAllCollections()) {
            for (DocumentaryUnitDescription description : doc.getDocumentDescriptions()) {
                for (AccessPoint relationship : description.getAccessPoints()) {
                    if (accessPointTypes.isEmpty() || accessPointTypes
                            .contains(relationship.getRelationshipType())) {
                        String trimmedName = relationship.getName().trim();
                        String identifier = getIdentifier(relationship);
                        String prior = conceptIdentifierNames.get(identifier);
                        if (identifier.isEmpty() || trimmedName.isEmpty()) {
                            logger.warn("Ignoring empty access point name");
                        } else if (prior != null && !prior.equals(trimmedName)) {
                            logger.warn("Concept name/slug collision: '{}' -> '{}'", trimmedName,
                                    prior);
                        } else {
                            conceptIdentifierNames.put(identifier, trimmedName);
                            identifierConcept.put(identifier, Optional.<Concept>absent());
                            int count = identifierCount.containsKey(identifier)
                                    ? identifierCount.get(identifier)
                                    : 0;
                            identifierCount.put(identifier, count + 1);
                        }
                    }
                }
            }
        }

        // Abort if we've got no concepts - this avoids creating
        // an event unnecessarily...
        if (!willCreateItems(identifierCount, excludeSingles)) {
            return 0L;
        }

        // Now create concepts for all the names
        ActionManager actionManager = new ActionManager(graph);
        ActionManager.EventContext conceptEvent = actionManager
                .setScope(vocabulary)
                .newEventContext(user, EventTypes.creation, logMessage);
        CrudViews<Concept> conceptMaker = new CrudViews<>(graph, Concept.class, vocabulary);

        for (Map.Entry<String, String> idName : conceptIdentifierNames.entrySet()) {
            String identifier = idName.getKey();
            String name = idName.getValue();

            // if we're excluding "unique" access points, skip this...
            if (identifierCount.get(identifier) < 2 && excludeSingles) {
                continue;
            }

            Bundle conceptBundle = Bundle.Builder.withClass(EntityClass.CVOC_CONCEPT)
                    .addDataValue(Ontology.IDENTIFIER_KEY, identifier)
                    .addRelation(Ontology.DESCRIPTION_FOR_ENTITY, Bundle.Builder
                            .withClass(EntityClass.CVOC_CONCEPT_DESCRIPTION)
                            .addDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, defaultLanguageCode)
                            .addDataValue(Ontology.NAME_KEY, name)
                            .build())
                    .build();

            try {
                Concept concept = conceptMaker.create(conceptBundle, user);
                concept.setVocabulary(vocabulary);
                identifierConcept.put(identifier, Optional.fromNullable(concept));
                conceptEvent.addSubjects(concept);
            } catch (ValidationError validationError) {
                // If this happens it is most likely because two access points
                // slugified to the same name due to the removal of diacritics
                // etc. The createOrUpdate operation currently doesn't seem to
                // work in the same transaction (possibly due to the graph index
                // not being flushed), so for the moment we're just going to log
                // the error and continue.
                logger.warn("Id/name collision error: '{}' -> '{}' ('{}')", identifier, name,
                        conceptIdentifierNames.get(identifier));
                logger.error("Link integrity error: ", validationError);
                if (!tolerant) {
                    throw validationError;
                }
            }
        }

        conceptEvent.commit();

        // Now link the concepts with elements having the access point from
        // which the concept originally derived.
        ActionManager.EventContext linkEvent = actionManager
                .newEventContext(user, EventTypes.creation, logMessage);
        CrudViews<Link> linkMaker = new CrudViews<>(graph, Link.class);

        long linkCount = 0L;
        for (DocumentaryUnit doc : repository.getAllCollections()) {
            for (DocumentaryUnitDescription description : doc.getDocumentDescriptions()) {
                for (AccessPoint relationship : description.getAccessPoints()) {
                    if (accessPointTypes.isEmpty() || accessPointTypes
                            .contains(relationship.getRelationshipType())) {

                        String identifier = getIdentifier(relationship);
                        // if we're excluding "unique" access points, skip this...
                        if (identifierCount.get(identifier) < 2 && excludeSingles) {
                            continue;
                        }

                        Optional<Concept> conceptOpt = identifierConcept.get(identifier);
                        if (conceptOpt != null && conceptOpt.isPresent()) {
                            Concept concept = conceptOpt.get();
                            Bundle linkBundle = Bundle.Builder.withClass(EntityClass.LINK)
                                    .addDataValue(Ontology.LINK_HAS_TYPE, LINK_TYPE)
                                    .build();
                            Link link = linkMaker.create(linkBundle, user);
                            link.addLinkTarget(doc);
                            link.addLinkTarget(concept);
                            link.addLinkBody(relationship);
                            linkEvent.addSubjects(link);
                            linkCount++;
                        }
                    }
                }
            }
        }

        linkEvent.commit();

        return linkCount;
    }

    /**
     * Set the linker to ignore concepts which would only connect to a single
     * item.
     *
     * @param excludeSingles a boolean value
     * @return a new linker object
     */
    public Linker withExcludeSingles(boolean excludeSingles) {
        return new Linker(graph, accessPointTypes, DEFAULT_LANG,
                logMessage, tolerant, excludeSingles);
    }

    /**
     * Set the linker to proceed even if there are integrity errors caused
     * by two distinct concept names slugifying to the
     * same string.
     *
     * @param tolerant a boolean value
     * @return a new linker object
     */
    public Linker withTolerant(boolean tolerant) {
        return new Linker(graph, accessPointTypes, DEFAULT_LANG,
                logMessage, tolerant, excludeSingles);
    }

    /**
     * Set the default language code to use for concept descriptions.
     *
     * @param defaultLanguageCode a three-letter ISO-639-2 code
     * @return a new linker object
     */
    public Linker withDefaultLanguage(String defaultLanguageCode) {
        return new Linker(graph, accessPointTypes, checkNotNull(defaultLanguageCode),
                logMessage, tolerant, excludeSingles);
    }

    /**
     * Set the log message for the created items.
     *
     * @param logMessage    a descriptive string
     * @return a new linker object
     */
    public Linker withLogMessage(String logMessage) {
        return new Linker(graph, accessPointTypes, checkNotNull(defaultLanguageCode),
                Optional.fromNullable(logMessage), tolerant, excludeSingles);
    }

    /**
     * Set the log message for the created items.
     *
     * @param logMessage    a descriptive string
     * @return a new linker object
     */
    public Linker withLogMessage(Optional<String> logMessage) {
        return new Linker(graph, accessPointTypes, checkNotNull(defaultLanguageCode),
                checkNotNull(logMessage), tolerant, excludeSingles);
    }

    /**
     * Set the linker to include <b>only</b> the given access point types, discarding
     * those already configured. If an empty list is given, all access point types
     * will be included.
     *
     * @param accessPointTypes a list of access point types
     * @return a new linker object
     */
    public Linker withAccessPointTypes(List<String> accessPointTypes) {
        return new Linker(graph, Sets.newHashSet(checkNotNull(accessPointTypes)),
                defaultLanguageCode, logMessage, tolerant, excludeSingles);
    }

    /**
     * Set the linker to include the given access point type, in addition
     * to those already configured.
     *
     * @param accessPointType an access point type string
     * @return a new linker object
     */
    public Linker withAccessPointType(String accessPointType) {
        Set<String> tmp = Sets.newHashSet(checkNotNull(accessPointTypes));
        tmp.add(accessPointType);
        return new Linker(graph, tmp, defaultLanguageCode,
                logMessage, tolerant, excludeSingles);
    }

    // Helpers...

    private static boolean willCreateItems(Map<String, Integer> identifierCounts, boolean excludeSingles) {
        if (identifierCounts.isEmpty()) {
            return false;
        } else if (excludeSingles) {
            Integer maxCount = 0;
            for (Integer c : identifierCounts.values()) {
                if (c != null && c > maxCount) {
                    maxCount = c;
                }
            }
            if (maxCount < 2) {
                return false;
            }
        }
        return true;
    }

    private static String getIdentifier(AccessPoint relationship) {
        return Slugify.slugify(relationship.getName().trim())
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }
}