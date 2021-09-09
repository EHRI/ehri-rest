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

package eu.ehri.project.importers;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.ehri.project.persistence.ActionManager;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Class that serves as a manifest for an import batch,
 * detailing how many items were created and updated,
 * and how many failed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportLog {

    private final Multimap<String, String> createdKeys = HashMultimap.create();
    private final Multimap<String, String> updatedKeys = HashMultimap.create();
    private final Multimap<String, String> unchangedKeys = HashMultimap.create();
    private final String logMessage;
    private final Map<String, String> errors = Maps.newHashMap();
    private final String eventId;


    /**
     * Constructor.
     *
     * @param logMessage a log message
     */
    public ImportLog(String logMessage) {
        this(logMessage, null);
    }

    public ImportLog(String logMessage, String eventId) {
        this.logMessage = logMessage;
        this.eventId = eventId;
    }

    @JsonCreator
    public ImportLog(
            @JsonProperty("message") String logMessage,
            @JsonProperty("created_keys") Map<String, Collection<String>> createdKeys,
            @JsonProperty("updated_keys") Map<String, Collection<String>> updatedKeys,
            @JsonProperty("unchanged_keys") Map<String, Collection<String>> unchangedKeys,
            @JsonProperty("errors") Map<String, String> errors,
            @JsonProperty("event") String eventId) {
        this(logMessage, eventId);
        createdKeys.forEach(this.createdKeys::putAll);
        unchangedKeys.forEach(this.unchangedKeys::putAll);
        updatedKeys.forEach(this.updatedKeys::putAll);
        this.errors.putAll(errors);
    }

    /**
     * Increment the creation count.
     *
     * @param sourceKey the source specifier key
     * @param itemId    the newly-created item ID
     */
    public void addCreated(String sourceKey, String itemId) {
        Preconditions.checkNotNull(sourceKey);
        createdKeys.put(sourceKey, itemId);
    }

    /**
     * Increment the update count.
     *
     * @param sourceKey the source specifier key
     * @param itemId    the newly-updated item ID
     */
    public void addUpdated(String sourceKey, String itemId) {
        Preconditions.checkNotNull(sourceKey);
        updatedKeys.put(sourceKey, itemId);
    }

    /**
     * Increment the unchanged count.
     *
     * @param sourceKey the source specifier key
     * @param itemId    the unchanged item ID
     */
    public void addUnchanged(String sourceKey, String itemId) {
        Preconditions.checkNotNull(sourceKey);
        unchangedKeys.put(sourceKey, itemId);
    }

    /**
     * @return returns the number of created items
     */
    public int getCreated() {
        return createdKeys.size();
    }

    /**
     * @return the items created for each input key
     */
    public Multimap<String, String> getCreatedKeys() {
        return createdKeys;
    }

    /**
     * @return returns the number of updated items
     */
    public int getUpdated() {
        return updatedKeys.size();
    }

    /**
     * @return the items updated for each input key
     */
    public Multimap<String, String> getUpdatedKeys() {
        return updatedKeys;
    }

    /**
     * @return returns the number of unchanged items
     */
    public int getUnchanged() {
        return unchangedKeys.size();
    }

    /**
     * @return the items unchanged for each input key
     */
    public Multimap<String, String> getUnchangedKeys() {
        return unchangedKeys;
    }

    /**
     * @return the number of errored item imports
     */
    public int getErrored() {
        return errors.size();
    }

    /**
     * @return the import errors
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * Indicate that importing the item with the given id
     * failed with the given error.
     *
     * @param item  The item
     * @param error The error that occurred
     */
    public void addError(String item, String error) {
        errors.put(item, error);
    }

    /**
     * Indicated whether the import succeeded at all,
     * in terms of items created/updated.
     *
     * @return returns whether the import succeeded
     */
    public boolean hasDoneWork() {
        return createdKeys.size() > 0 || updatedKeys.size() > 0;
    }

    /**
     * @return returns the number of items that were either created or updated.
     */
    public int getChanged() {
        return createdKeys.size() + updatedKeys.size();
    }

    /**
     * Return a new log with the given event ID.
     *
     * @param id a system event ID
     * @return a new import log
     */
    public ImportLog withEventId(String id) {
        return new ImportLog(
                logMessage,
                createdKeys.asMap(),
                updatedKeys.asMap(),
                unchangedKeys.asMap(),
                errors,
                id
        );
    }

    /**
     * Return a new log updated with an event ID if change events
     * have take place. Otherwise the same log is returned and the
     * event context left uncommitted.
     *
     * @param context an event content object
     * @return either an updated log object with the ID of the committed
     * event context, or the same log
     */
    public ImportLog committing(ActionManager.EventContext context) {
        return hasDoneWork()
                ? withEventId(context.commit().getId())
                : this;
    }

    @JsonValue
    public Map<String, Object> getData() {
        Map<String, Object> data = Maps.newHashMap();
        data.put("created", createdKeys.size());
        data.put("created_keys", createdKeys.asMap());
        data.put("updated", updatedKeys.size());
        data.put("updated_keys", updatedKeys.asMap());
        data.put("unchanged", unchangedKeys.size());
        data.put("unchanged_keys", unchangedKeys.asMap());
        data.put("errors", errors);
        data.put("message", logMessage);
        data.put("event", eventId);
        return data;
    }

    @JsonIgnore
    public Optional<String> getLogMessage() {
        return Optional.of(logMessage);
    }

    @JsonIgnore
    public Optional<String> getEventId() {
        return Optional.of(eventId);
    }

    @Override
    public String toString() {
        return String.format(
                "Created: %d, Updated: %d, Unchanged: %d, Errors: %d",
                createdKeys.size(), updatedKeys.size(), unchangedKeys.size(), errors.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportLog importLog = (ImportLog) o;
        return Objects.equals(logMessage, importLog.logMessage) &&
                Objects.equals(eventId, importLog.eventId) &&
                createdKeys.equals(importLog.createdKeys) &&
                updatedKeys.equals(importLog.updatedKeys) &&
                unchangedKeys.equals(importLog.unchangedKeys) &&
                errors.equals(importLog.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdKeys, updatedKeys, unchangedKeys, logMessage, errors, eventId);
    }
}
