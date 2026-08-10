/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.managers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.PostImportCallback;
import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Base class for import managers that parse an input stream into a sequence
 * of flat property maps, each of which is imported as a single item. Concrete
 * subclasses (e.g. CSV, JSON) supply only the parsing; item import and
 * error handling are shared here.
 */
public abstract class MapImportManager extends AbstractImportManager {

    private static final Logger logger = LoggerFactory.getLogger(MapImportManager.class);

    protected MapImportManager(FramedGraph<?> framedGraph,
                               PermissionScope permissionScope,
                               Actioner actioner,
                               Class<? extends ItemImporter<?, ?>> importerClass,
                               ImportOptions options,
                               List<PreImportCallback> preCallbacks,
                               List<PostImportCallback> postCallbacks) {
        super(framedGraph, permissionScope, actioner, importerClass, options, preCallbacks, postCallbacks);
    }

    /**
     * Import a single already-parsed property map as one item, recording any
     * validation error in the log and, unless the manager is tolerant,
     * re-throwing it to abort the import.
     *
     * @param importer the item importer
     * @param data     the item's property map
     * @param tag      an identifier for the import source
     * @param log      the import log
     * @throws ValidationError if the item is invalid and the manager is not tolerant
     */
    @SuppressWarnings("unchecked")
    protected void importDataMap(ItemImporter<?, ?> importer, Map<String, Object> data, String tag, ImportLog log)
            throws ValidationError {
        try {
            ((ItemImporter<Map<String, Object>, ?>) importer).importItem(data);
        } catch (ValidationError e) {
            // Record the failure in the log so it's reflected in the errored count,
            // then either continue (tolerant) or re-throw (strict).
            log.addError(e.getBundle().getId(), e.getErrorSet().toString());
            if (isTolerant()) {
                logger.error(String.format("Validation error importing item: '%s'", tag), e);
            } else {
                throw e;
            }
        }
    }
}
