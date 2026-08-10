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

package eu.ehri.project.importers.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.PostImportCallback;
import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Import manager to use with JSON files containing a flat object of data fields.
 * When used to import DocumentaryUnits, make sure to have a 'sourceFileId' field as well.
 */
public class JsonImportManager extends AbstractImportManager {

    private static final Logger logger = LoggerFactory.getLogger(JsonImportManager.class);

    private JsonImportManager(FramedGraph<?> framedGraph,
                              PermissionScope permissionScope,
                              Actioner actioner,
                              Class<? extends ItemImporter<?, ?>> importerClass,
                              ImportOptions options,
                              List<PreImportCallback> preCallbacks,
                              List<PostImportCallback> callbacks) {
        super(framedGraph, permissionScope, actioner, importerClass, options, preCallbacks, callbacks);
    }

    public static JsonImportManager create(FramedGraph<?> framedGraph,
                                           PermissionScope permissionScope, Actioner actioner,
                                           Class<? extends ItemImporter<?, ?>> importerClass, ImportOptions options) {
        return new JsonImportManager(framedGraph, permissionScope, actioner, importerClass, options, Lists.newArrayList(), Lists.newArrayList());
    }

    /**
     * Import JSON from the given InputStream, as part of the given action.
     *
     * @param stream  the input stream
     * @param context the event context in which the ingest operation is taking place
     * @param log     an import log instance
     */
    @Override
    protected void importInputStream(InputStream stream, String tag, final ActionManager.EventContext context, final ImportLog log)
            throws IOException, ValidationError, InputParseError {

        try {
            ItemImporter<?, ?> importer = importerClass
                    .getConstructor(FramedGraph.class, PermissionScopeFinder.class, Actioner.class, ImportOptions.class, ImportLog.class)
                    .newInstance(framedGraph, scopeFinder, actioner, options, log);
            logger.trace("importer of class {}", importer.getClass());

            registerCallbacks(importer);
            importer.addPostCallback(mutation -> defaultImportCallback(log, tag, context, mutation));
            importer.addErrorCallback(ex -> defaultErrorCallback(log, ex));

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(stream);

                if (root.isArray()) {
                    List<Map<String, Object>> list = mapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
                    importMultipleMaps(importer, list, tag, log);
                } else if (root.isObject()) {
                    Map<String, Object> map = mapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
                    importSingleMap(importer, map, tag, log);
                } else {
                    throw new IllegalArgumentException("Expected a JSON object or array, got: " + root.getNodeType());
                }
            } catch (IllegalArgumentException e) {
                throw new InputParseError(e.getMessage());
            }
        } catch (IllegalAccessException | InvocationTargetException |
                 InstantiationException | NoSuchMethodException |
                 ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleError(ImportLog log, String tag, ValidationError error) throws ValidationError {
        // Record the failure in the log so it's reflected in the errored count,
        // then either continue (tolerant) or re-throw (strict).
        log.addError(error.getBundle().getId(), error.getErrorSet().toString());
        if (isTolerant()) {
            logger.error(String.format("Validation error importing item: '%s'", tag), error);
        } else {
            throw error;
        }
    }

    private void importSingleMap(ItemImporter<?, ?> importer, Map<String, Object> itemData, String tag, ImportLog log) throws ValidationError {
        try {
            Map<String, Object> importData = Maps.newHashMap();
            for (Map.Entry<String, Object> entry : itemData.entrySet()) {
                if (entry.getValue() instanceof List<?>) {
                    List<?> arr = (List<?>) entry.getValue();
                    for (Object arrValue : arr) {
                        ImportHelpers.putPropertyInGraph(importData, entry.getKey(), String.valueOf(arrValue));
                    }
                } else if (entry.getValue() != null) {
                    ImportHelpers.putPropertyInGraph(importData, entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            ((ItemImporter<Map<String, Object>, ?>) importer).importItem(importData);
        } catch (ValidationError e) {
            handleError(log, tag, e);
        }
    }

    private void importMultipleMaps(ItemImporter<?, ?> importer, List<Map<String, Object>> listData, String tag, ImportLog log) throws ValidationError {
        for (Map<String, Object> itemData : listData) {
            importSingleMap(importer, itemData, tag, log);
        }
    }

    public JsonImportManager withPreCallback(PreImportCallback callback) {
        List<PreImportCallback> newCbs = com.google.common.collect.Lists.newArrayList(preCallbacks);
        newCbs.add(callback);
        return new JsonImportManager(framedGraph, permissionScope, actioner,
                importerClass, options, newCbs, postCallbacks);
    }

    public JsonImportManager withCallback(PostImportCallback callback) {
        List<PostImportCallback> newCbs = com.google.common.collect.Lists.newArrayList(postCallbacks);
        newCbs.add(callback);
        return new JsonImportManager(framedGraph, permissionScope, actioner,
                importerClass, options, preCallbacks, newCbs);
    }
}
