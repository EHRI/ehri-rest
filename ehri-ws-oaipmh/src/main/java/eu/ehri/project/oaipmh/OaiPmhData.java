/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.oaipmh;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.oaipmh.errors.OaiPmhError;
import eu.ehri.project.utils.LanguageHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Data fetcher for OAI-PMH requests.
 */
public class OaiPmhData {

    private static final Logger log = LoggerFactory.getLogger(OaiPmhData.class);
    private static final Splitter hierarchySplitter = Splitter.on('-');
    private static final Joiner hierarchyJoiner = Joiner.on('-');
    private static final Splitter setSpecSplitter = Splitter.on(':');
    private static final Joiner setSpecJoiner = Joiner.on(':');

    private final Api api;
    private final boolean sort;

    private OaiPmhData(Api api, boolean sort) {
        this.api = api;
        this.sort = sort;
    }

    public static OaiPmhData create(Api api, boolean sort) {
        return new OaiPmhData(api, sort);
    }

    public static OaiPmhData create(Api api) {
        return create(api, false);
    }

    QueryApi.Page<OaiPmhSet> getSets(OaiPmhState state) {
        Stream<OaiPmhSet> setStream = getSets();
        long count = state.hasLimit() ? getSets().count() : -1L;

        Stream<OaiPmhSet> offsetStream = setStream.skip(state.getOffset());
        Stream<OaiPmhSet> limitedSetStream = state.hasLimit()
                ? offsetStream.limit(state.getLimit())
                : offsetStream;
        return new QueryApi.Page<>(limitedSetStream::iterator,
                state.getOffset(), state.getLimit(), count);
    }

    QueryApi.Page<DocumentaryUnit> getFilteredDocumentaryUnits(OaiPmhState state) throws OaiPmhError {
        String defaultTimestamp = api.actionManager().getEventRoot().getTimestamp();
        Iterable<DocumentaryUnit> filtered = Iterables.filter(
                getDocumentaryUnits(state.getSetSpec()), timeFilterItems(state.getFrom(), state.getUntil(), defaultTimestamp));
        QueryApi q = api.query().withOffset(state.getOffset()).withLimit(state.getLimit());
        QueryApi sortQ = sort ? q.orderBy(EntityType.ID_KEY, QueryApi.Sort.ASC) : q;
        return sortQ.page(filtered, DocumentaryUnit.class);
    }

    OaiPmhRecordResult getRecord(OaiPmhState state) throws OaiPmhError {
        try {
            return OaiPmhRecordResult.of(api.get(state.getIdentifier(), DocumentaryUnit.class));
        } catch (ItemNotFound e) {
            Optional<Version> deletedOpt = api.versionManager().versionAtDeletion(state.getIdentifier());
            if (deletedOpt.isPresent()) {
                return OaiPmhRecordResult.deleted(getDeletedRecord(deletedOpt.get()));
            } else {
                return OaiPmhRecordResult.invalid();
            }
        }
    }

    Iterable<OaiPmhDeleted> getFilteredDeletedDocumentaryUnits(final OaiPmhState state) {
        String setSpec = state.getSetSpec();
        if (setSpec == null || setSpec.trim().isEmpty()) {
            return getDeletedDocumentaryUnits(state.getFrom(), state.getUntil());
        } else {
            List<String> specParts = Splitter.on(':').splitToList(setSpec);
            return Iterables.filter(getDeletedDocumentaryUnits(state.getFrom(), state.getUntil()), d -> {
                List<String> sets = d.getSets();
                if (specParts.size() == 1 && sets.get(0).equals(specParts.get(0))) {
                    return true;
                } else if (specParts.size() == 2 && sets.get(1).equals(specParts.get(1))) {
                    return true;
                }
                return false;
            });
        }
    }

    private OaiPmhDeleted getDeletedRecord(Version version) {
        // FIXME: This is terrible but the only current way to determine set membership
        // for deleted items - rely on the global ID to determine country and repository
        log.trace("Calculating deleted item for {}", version.getEntityId());
        List<String> countryAndRepo = hierarchySplitter.limit(2).splitToList(version.getEntityId());
        List<String> sets = ImmutableList.of(countryAndRepo.get(0),
                setSpecJoiner.join(countryAndRepo.get(0), hierarchyJoiner.join(countryAndRepo)));
        ZonedDateTime deletedAt = ZonedDateTime.parse(version.getTriggeringEvent().getTimestamp());
        return new OaiPmhDeleted(version.getEntityId(), deletedAt, sets);
    }

    String getEarliestTimestamp() {
        return api.actionManager().getEventRoot().getTimestamp();
    }

    private Iterable<DocumentaryUnit> getDocumentaryUnits(String setSpec) throws OaiPmhError {
        return (setSpec == null || setSpec.trim().isEmpty())
                ? getDocumentaryUnits()
                : getDocumentaryUnitsFromSpecs(setSpec);
    }

    private Iterable<DocumentaryUnit> getDocumentaryUnitsFromSpecs(String setSpec) throws OaiPmhError {
        try {
            assert setSpec != null;
            List<String> specParts = setSpecSplitter.splitToList(setSpec);
            return specParts.size() == 1
                    ? api.get(specParts.get(0), Country.class).getTopLevelDocumentaryUnits()
                    : api.get(specParts.get(1), Repository.class).getTopLevelDocumentaryUnits();
        } catch (ItemNotFound e) {
            // FIXME: Should throw an invalid argument here instead
            throw new OaiPmhError(ErrorCode.badArgument, "Invalid set spec: " + setSpec);
        }
    }

    private Iterable<DocumentaryUnit> getDocumentaryUnits() {
        QueryApi.Page<Country> countries = getQuery().page(EntityClass.COUNTRY, Country.class);
        return Iterables.concat(Iterables.transform(countries, Country::getTopLevelDocumentaryUnits));
    }

    private QueryApi getQuery() {
        return api.query().withStreaming(true).withLimit(-1)
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC);
    }

    private Iterable<OaiPmhDeleted> getDeletedDocumentaryUnits(String from, String until) {
        Iterable<OaiPmhDeleted> transform = Iterables.transform(
                api.versionManager().versionsAtDeletion(EntityClass.DOCUMENTARY_UNIT, from, until),
                this::getDeletedRecord);
        return transform;
    }

    private Stream<OaiPmhSet> getSets() {
        Stream<Country> stream = streamOf(getQuery().page(EntityClass.COUNTRY, Country.class));
        return stream
                .filter(c -> c.getTopLevelDocumentaryUnits().iterator().hasNext())
                .flatMap(ct -> {
                    String countryId = ct.getId();
                    String countryName = LanguageHelpers.countryCodeToName(countryId);
                    OaiPmhSet countrySet = new OaiPmhSet(countryId, countryName,
                            "All items in repositories within country: " + countryName);
                    Stream<OaiPmhSet> repoSets = streamOf(getQuery().page(ct.getRepositories(), Repository.class))
                            .filter(r -> r.getTopLevelDocumentaryUnits().iterator().hasNext())
                            .map(r -> {
                                String repoName = r.getDescriptions().iterator().hasNext()
                                        ? r.getDescriptions().iterator().next().getName()
                                        : null;
                                return new OaiPmhSet(countryId + ":" + r.getId(), repoName, "All items within repository: " + repoName);
                            });
                    return Stream.concat(Stream.of(countrySet), repoSets);
                });
    }

    // Helpers...

    private static <T> Stream<T> streamOf(Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }

    private static <E extends Accessible> Predicate<E> timeFilterItems(String from, String until, String defaultTimestamp) {
        return d -> {
            String ts = Optional.ofNullable(d.getLatestEvent())
                    .map(SystemEvent::getTimestamp).orElse(defaultTimestamp);
            return filterByTimestamp(from, until, ts);
        };
    }

    private static boolean filterByTimestamp(String from, String until, String timestamp) {
        return (from == null || from.compareTo(timestamp) < 0)
                && (until == null || until.compareTo(timestamp) >= 0);
    }
}
