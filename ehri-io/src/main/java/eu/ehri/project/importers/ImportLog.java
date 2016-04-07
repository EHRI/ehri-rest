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

package eu.ehri.project.importers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Class that serves as a manifest for an import batch,
 * detailing how many items were created and updated,
 * and how many failed.
 */
public class ImportLog {

    private int created;
    private int updated;
    private int unchanged;
    private final Optional<String> logMessage;
    private final Map<String, String> errors = Maps.newHashMap();


    /**
     * Constructor.
     *
     * @param logMessage a log message
     */
    public ImportLog(Optional<String> logMessage) {
        this.logMessage = logMessage;
    }

    @JsonCreator
    public ImportLog(Optional<String> logMessage, int created,
            int updated, int unchanged, Map<String, String> errors) {
        this(logMessage);
        this.created = created;
        this.unchanged = unchanged;
        this.updated = updated;
        this.errors.putAll(errors);
    }

    /**
     * Increment the creation count.
     */
    public void addCreated() {
        created++;
    }

    /**
     * Increment the update count.
     */
    public void addUpdated() {
        updated++;
    }

    /**
     * Increment the unchanged count.
     */
    public void addUnchanged() {
        unchanged++;
    }

    /**
     * @return returns the number of created items
     */
    public int getCreated() {
        return created;
    }

    /**
     * @return returns the number of updated items
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * @return returns the number of unchanged items
     */
    public int getUnchanged() {
        return unchanged;
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
        return created > 0 || updated > 0;
    }

    /**
     * @return returns the number of items that were either created or updated.
     */
    public int getChanged() {
        return created + updated;
    }

    @JsonValue
    public Map<String, Object> getData() {
        Map<String, Object> data = Maps.newHashMap();
        data.put("created", created);
        data.put("updated", updated);
        data.put("unchanged", unchanged);
        data.put("errors", errors);
        data.put("message", logMessage.orNull());
        return data;
    }

    public Optional<String> getLogMessage() {
        return logMessage;
    }

    public void printReport() {
        System.out.println(
                String.format(
                        "Created: %d, Updated: %d, Unchanged: %d, Errors: %d",
                        created, updated, unchanged, errors.size()));
    }
}
