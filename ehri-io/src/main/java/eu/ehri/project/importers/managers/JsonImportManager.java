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
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.PostImportCallback;
import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.apache.commons.compress.utils.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Import manager to use with JSON files containing a flat object of data fields,
 * or an array of such objects.
 * When used to import DocumentaryUnits, make sure to have a 'sourceFileId' field as well.
 */
public class JsonImportManager extends MapImportManager {

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

        ItemImporter<?, ?> importer = initImporter(tag, context, log);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(stream);

            if (root.isArray()) {
                List<Map<String, Object>> list = mapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> itemData : list) {
                    importDataMap(importer, toImportData(itemData), tag, log);
                }
            } else if (root.isObject()) {
                Map<String, Object> map = mapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
                importDataMap(importer, toImportData(map), tag, log);
            } else {
                throw new IllegalArgumentException("Expected a JSON object or array, got: " + root.getNodeType());
            }
        } catch (IllegalArgumentException e) {
            throw new InputParseError(e.getMessage());
        }
    }

    /**
     * Flatten a parsed JSON object into a graph property map, expanding array
     * values into repeated properties and skipping null values.
     */
    private Map<String, Object> toImportData(Map<String, Object> itemData) {
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
        return importData;
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
