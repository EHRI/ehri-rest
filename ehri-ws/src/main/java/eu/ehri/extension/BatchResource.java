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

package eu.ehri.extension;

import com.google.common.base.Charsets;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.json.BatchOperations;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.utils.Table;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

/**
 * Resource class for import endpoints.
 */
@Path(BatchResource.ENDPOINT)
public class BatchResource extends AbstractResource {

    public static final String ENDPOINT = "batch";

    private static final Logger logger = LoggerFactory.getLogger(BatchResource.class);

    public BatchResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Update a batch of items via JSON containing (partial)
     * data bundles.
     *
     * @param scope       the ID of there item's permission scope
     * @param tolerant    whether to allow individual validation failures
     * @param version     whether to create a version prior to delete
     * @param log         an optional log message
     * @param inputStream a JSON document containing partial bundles containing
     *                    the needed data transformations
     * @return an import log describing the changes committed
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("update")
    public ImportLog batchUpdate(
            @QueryParam(SCOPE_PARAM) String scope,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("true") @QueryParam(VERSION_PARAM) Boolean version,
            @QueryParam(LOG_PARAM) String log,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream inputStream)
            throws IOException, ItemNotFound, ValidationError, DeserializationError {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
            PermissionScope parent = scope != null
                    ? manager.getEntity(scope, PermissionScope.class)
                    : null;
            ImportLog importLog = new BatchOperations(graph, parent, version, tolerant, Collections.emptyList())
                    .batchUpdate(inputStream, user, getLogMessage(log));
            if (commit) {
                logger.debug("Committing batch update transaction...");
                tx.success();
            }
            return importLog;
        }
    }

    /**
     * Delete a batch of objects via JSON containing their IDs.
     *
     * @param scope   the ID of there item's permission scope
     * @param version whether to create a version prior to delete
     * @param log     an optional log message.
     * @return the number of items deleted
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("delete")
    public String batchDelete(
            @QueryParam(SCOPE_PARAM) String scope,
            @DefaultValue("true") @QueryParam(VERSION_PARAM) Boolean version,
            @QueryParam(LOG_PARAM) String log,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            Table ids)
            throws IOException, DeserializationError {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
            PermissionScope parent = scope != null
                    ? manager.getEntity(scope, PermissionScope.class)
                    : null;
            int done = new BatchOperations(graph, parent, version, false, Collections.emptyList())
                    .batchDelete(ids.column(0), user, getLogMessage(log));
            if (commit) {
                logger.debug("Committing batch delete transaction...");
                tx.success();
            }
            return String.valueOf(done);
        } catch (ItemNotFound e) {
            throw new DeserializationError("Unable to locate item with ID: " + e.getValue());
        }
    }


    // Helpers

    private Optional<String> getLogMessage(String logMessagePathOrText) throws IOException {
        if (logMessagePathOrText == null || logMessagePathOrText.trim().isEmpty()) {
            return getLogMessage();
        } else {
            java.nio.file.Path fileTest = Paths.get(logMessagePathOrText);
            if (Files.isRegularFile(fileTest)) {
                return Optional.of(new String(Files.readAllBytes(fileTest), Charsets.UTF_8));
            } else {
                return Optional.of(logMessagePathOrText);
            }
        }
    }

}
