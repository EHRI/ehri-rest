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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
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
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Import manager to use with CSV files.
 * When used to import DocumentaryUnits, make sure to have a 'sourceFileId' column as well.
 */
public class CsvImportManager extends AbstractImportManager {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportManager.class);

    private CsvImportManager(FramedGraph<?> framedGraph,
                             PermissionScope permissionScope,
                             Actioner actioner,
                             Class<? extends ItemImporter<?, ?>> importerClass,
                             ImportOptions options,
                             List<PreImportCallback> preCallbacks,
                             List<PostImportCallback> callbacks) {
        super(framedGraph, permissionScope, actioner, importerClass, options, preCallbacks, callbacks);
    }

    public static CsvImportManager create(FramedGraph<?> framedGraph,
                                          PermissionScope permissionScope, Actioner actioner,
                                          Class<? extends ItemImporter<?, ?>> importerClass, ImportOptions options) {
        return new CsvImportManager(framedGraph, permissionScope, actioner, importerClass, options, Lists.newArrayList(), Lists.newArrayList());
    }

    /**
     * Import CSV from the given InputStream, as part of the given action.
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

            CsvSchema schema = CsvSchema.emptySchema()
                    .withColumnSeparator(options.defaultFieldSep)
                    .withHeader();
            ObjectReader reader = new CsvMapper().readerFor(Map.class).with(schema);
            // Despite Jackson having support for array elements in the CsvSchema, we're only
            // parsing as a simple map, so we need to do the splitting ourselves.
            final Splitter arraySplitter = Splitter.on(options.defaultArraySep).trimResults().omitEmptyStrings();

            try (InputStreamReader s = new InputStreamReader(stream, Charsets.UTF_8);
                 MappingIterator<Map<String, String>> valueIterator = reader.readValues(s)) {
                while (valueIterator.hasNext()) {
                    Map<String, String> rawData = valueIterator.next();
                    Map<String, Object> dataMap = Maps.newHashMap();
                    for (Map.Entry<String, String> entry : rawData.entrySet()) {
                        final String property = entry.getKey().replaceAll("\\s", "");
                        final String value = entry.getValue();

                        // FIXME: ideally we'd know for sure if the field is multi-valued?
                        // For now just assume if the array separator is present, it's an array.
                        if (value.contains(options.defaultArraySep)) {
                            arraySplitter.split(value)
                                    .forEach(v -> ImportHelpers.putPropertyInGraph(dataMap, property, v));
                        } else {
                            ImportHelpers.putPropertyInGraph(dataMap, property, value);
                        }
                    }
                    try {
                        ((ItemImporter<Map<String, Object>, ?>) importer).importItem(dataMap);
                    } catch (ValidationError e) {
                        if (isTolerant()) {
                            logger.error(String.format("Validation error importing item: '%s'", tag), e);
                        } else {
                            throw e;
                        }
                    }
                }
                // When an error reading CSV data is thrown it is -- counterintuitively --
                // a JSON mapping exception wrapping a CsvMappingException.
            } catch (RuntimeJsonMappingException e) {
                throw new InputParseError(e.getCause().getMessage());
            }
        } catch (IllegalAccessException | InvocationTargetException |
                 InstantiationException | NoSuchMethodException |
                 ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public CsvImportManager withPreCallback(PreImportCallback callback) {
        List<PreImportCallback> newCbs = com.google.common.collect.Lists.newArrayList(preCallbacks);
        newCbs.add(callback);
        return new CsvImportManager(framedGraph, permissionScope, actioner,
                importerClass, options, newCbs, extraCallbacks);
    }

    public CsvImportManager withCallback(PostImportCallback callback) {
        List<PostImportCallback> newCbs = com.google.common.collect.Lists.newArrayList(extraCallbacks);
        newCbs.add(callback);
        return new CsvImportManager(framedGraph, permissionScope, actioner,
                importerClass, options, preCallbacks, newCbs);
    }
}
