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

package eu.ehri.project.importers.managers;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Import manager to use with CSV files.
 * When used to import DocumentaryUnits, make sure to have a 'sourceFileId' column as well.
 */
public class CsvImportManager extends AbstractImportManager {

    public static final Character VALUE_DELIMITER = ';';

    private static final Logger logger = LoggerFactory.getLogger(CsvImportManager.class);

    public CsvImportManager(FramedGraph<?> framedGraph,
            PermissionScope permissionScope, Actioner actioner, Class<? extends AbstractImporter> importerClass) {
        super(framedGraph, permissionScope, actioner, importerClass);
    }

    /**
     * Import CSV from the given InputStream, as part of the given action.
     *
     * @param ios          The input stream
     * @param eventContext The event context in which the ingest is happening
     * @param log          An import log instance
     * @throws IOException
     * @throws ValidationError
     */
    @Override
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError, InputParseError {

        try {
            AbstractImporter importer = importerClass
                    .getConstructor(FramedGraph.class, PermissionScope.class, ImportLog.class)
                    .newInstance(framedGraph, permissionScope, log);
            logger.debug("importer of class " + importer.getClass());

            importer.addCallback(new ImportCallback() {
                public void itemImported(Mutation<? extends Accessible> mutation) {
                    switch (mutation.getState()) {
                        case CREATED:
                            logger.info("Item created: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addCreated();
                            break;
                        case UPDATED:
                            logger.info("Item updated: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addUpdated();
                            break;
                        default:
                            log.addUnchanged();
                    }
                }
            });

            CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(VALUE_DELIMITER).withHeader();
            ObjectReader reader = new CsvMapper().readerFor(Map.class).with(schema);

            try (MappingIterator<Map<String, String>> valueIterator = reader
                    .readValues(new InputStreamReader(ios, "UTF-8"))) {
                while (valueIterator.hasNext()) {
                    Map<String, String> rawData = valueIterator.next();
                    Map<String, Object> dataMap = Maps.newHashMap();
                    for (Map.Entry<String, String> entry : rawData.entrySet()) {
                        Helpers.putPropertyInGraph(dataMap,
                                entry.getKey().replaceAll("\\s", ""), entry.getValue());
                    }
                    try {
                        importer.importItem(dataMap);
                    } catch (ValidationError e) {
                        if (isTolerant()) {
                            logger.error("Validation error importing item: {}", e);
                        } else {
                            throw e;
                        }
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException |
                InstantiationException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
