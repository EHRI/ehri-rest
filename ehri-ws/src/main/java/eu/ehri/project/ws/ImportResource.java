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

package eu.ehri.project.ws;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporterFactory;
import eu.ehri.project.importers.eac.EacHandler;
import eu.ehri.project.importers.eac.EacImporter;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.ead.EadSync;
import eu.ehri.project.importers.ead.SyncLog;
import eu.ehri.project.importers.eag.EagHandler;
import eu.ehri.project.importers.eag.EagImporter;
import eu.ehri.project.importers.exceptions.ImportValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.links.LinkImporter;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.utils.Table;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.shared.NoReaderForLangException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Resource class for import endpoints.
 */
@Path(ImportResource.ENDPOINT)
public class ImportResource extends AbstractResource {

    public static final String ENDPOINT = "import";

    private static final Logger logger = LoggerFactory.getLogger(ImportResource.class);
    private static final String DEFAULT_EAD_HANDLER = EadHandler.class.getName();
    private static final String DEFAULT_EAD_IMPORTER = EadImporter.class.getName();

    public static final String BASE_URI_PARAM = "baseURI";
    public static final String URI_SUFFIX_PARAM = "suffix";
    public static final String ALLOW_UPDATES_PARAM = "allow-update";
    public static final String USE_SOURCE_ID_PARAM = "use-source-id";
    public static final String HANDLER_PARAM = "handler";
    public static final String TAG_PARAM = "tag";
    public static final String IMPORTER_PARAM = "importer";
    public static final String PROPERTIES_PARAM = "properties";
    public static final String FORMAT_PARAM = "format";

    public ImportResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Import a SKOS file, of varying formats, as specified by the &quot;language&quot;
     * column of the file extensions table <a href="https://jena.apache.org/documentation/io/">here</a>.
     * <p>
     * Example:
     * <pre>
     *    <code>
     * curl -X POST \
     *      -H "X-User: mike" \
     *      --data-binary @skos-data.rdf \
     *      "http://localhost:7474/ehri/import/skos?scope=gb-my-vocabulary&amp;log=testing&amp;tolerant=true"
     *     </code>
     * </pre>
     *
     * @param scopeId      the id of the import scope (i.e. repository)
     * @param tolerant     whether or not to die on the first validation error
     * @param logMessage   log message for import. If this refers to an accessible local file
     *                     its contents will be used.
     * @param baseURI      a URI prefix common to ingested items that will be removed
     *                     from each item's URI to obtain the local identifier.
     * @param uriSuffix    a URI suffix common to ingested items that will be removed
     *                     from each item's URI to obtain the local identifier.
     * @param format       the RDF format of the POSTed data
     * @param commit       commit the operation to the database. The default
     *                     mode is to operate as a dry-run
     * @param allowUpdates allow the operation to update existing items
     * @param lang         the default language of description fields if not inferrable
     *                     the data
     * @param stream       a stream of SKOS data in a valid format.
     * @return a JSON object showing how many records were created,
     * updated, or unchanged.
     * @throws ItemNotFound         if the scope does not exist
     * @throws ValidationError      if data constraints are not met
     * @throws IOException          if an error occurs reading the input data
     * @throws DeserializationError if the input data is not well-formed
     */
    @POST
    @Path("skos")
    public ImportLog importSkos(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(BASE_URI_PARAM) String baseURI,
            @QueryParam(URI_SUFFIX_PARAM) String uriSuffix,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang,
            @QueryParam(FORMAT_PARAM) String format,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream stream)
            throws ItemNotFound, ValidationError, IOException, DeserializationError {
        try (final Tx tx = beginTx()) {
            // Get the current user from the Authorization header and the scope
            // from the query params...
            Actioner user = getCurrentActioner();
            Vocabulary scope = manager.getEntity(scopeId, Vocabulary.class);
            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, user, scope);

            ImportLog log = importer
                    .setFormat(format)
                    .setTolerant(tolerant)
                    .allowUpdates(allowUpdates)
                    .setDefaultLang(lang)
                    .setBaseURI(baseURI)
                    .setURISuffix(uriSuffix)
                    .importFile(stream, getLogMessage(logMessage).orElse(null));
            if (commit) {
                logger.debug("Committing SKOS import transaction...");
                tx.success();
            }
            return log;
        } catch (InputParseError e) {
            throw new DeserializationError("Unable to parse input: " + e.getMessage());
        } catch (NoReaderForLangException e) {
            throw new DeserializationError("Unable to read language: " + format);
        }
    }

    /**
     * Import a set of EAD files. The POST body can be one of:
     * <ul>
     * <li>a single EAD file</li>
     * <li>multiple EAD files in an archive such as tar, tar.gz, or zip</li>
     * <li>a plain test file containing local file paths</li>
     * <li>a JSON object consisting of file-name/URL pairs</li>
     * </ul>
     * The Content-Type header is used to distinguish the contents.
     * <p>
     * The way you would run with would typically be:
     * <pre>
     *    <code>
     *     curl -X POST \
     *      -H "X-User: mike" \
     *      --data-binary @ead-list.txt \
     *      "http://localhost:7474/ehri/import/ead?scope=my-repo-id&amp;log=testing&amp;tolerant=true"
     *
     * # NB: Data is sent using --data-binary to preserve line-breaks - otherwise
     * # it needs url encoding.
     *    </code>
     * </pre>
     * <p>
     * (Assuming <code>ead-list.txt</code> is a list of newline separated EAD file paths.)
     * <p>
     * (TODO: Might be better to use a different way of encoding the local file paths...)
     *
     * @param scopeId       the id of the import scope (i.e. repository)
     * @param useSourceId   by default a description will update an existing description if it
     *                      has the same language code. Setting this option to 'true' allows adding
     *                      multiple descriptions in the same language by taking into account the
     *                      value of the EAD ID.
     * @param logMessage    log message for import. If this refers to an accessible local file
     *                      its contents will be used.
     * @param handlerClass  the fully-qualified handler class name (defaults to EadHandler)
     * @param importerClass the fully-qualified import class name (defaults to EadImporter)
     * @param propertyFile  a local file path or URL pointing to an import properties configuration file.
     * @param defaultLang   the default description language, if not inferrable from the data
     * @param tag           if the data is a stream of XML a string &quot;tag&quot; can be
     *                      provided to identify the source, e.g. the name of the file from
     *                      which the stream derives. If a tag is not provided, or the
     *                      input data is not an XML stream, the default value of
     *                      &quot;-&quot; will be used
     * @param allowUpdates  allow the importer to update items that already exist. If it
     *                      attempts to do so without this option enabled an error will
     *                      be thrown
     * @param tolerant      whether or not to die on the first validation error
     * @param commit        commit the operation to the database. The default
     *                      mode is to operate as a dry-run
     * @param data          file data containing one of: a single EAD file,
     *                      multiple EAD files in an archive, a list of file URLs.
     *                      The Content-Type header is used to distinguish
     *                      the contents.
     * @return a JSON object showing how many records were created, updated, or unchanged.
     * @throws ItemNotFound          if the scope does not exist
     * @throws ImportValidationError if data constraints are not met
     * @throws IOException           if an error occurs reading the input data
     * @throws DeserializationError  if the input data is not well-formed
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("ead")
    public ImportLog importEad(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @DefaultValue("false") @QueryParam(USE_SOURCE_ID_PARAM) Boolean useSourceId,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String defaultLang,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(TAG_PARAM) @DefaultValue("-") String tag,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data)
            throws ItemNotFound, ImportValidationError, IOException, DeserializationError {

        try (final Tx tx = beginTx()) {
            checkPropertyFile(propertyFile);

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportOptions options = ImportOptions.create(
                    tolerant,
                    allowUpdates,
                    useSourceId,
                    defaultLang,
                    propertyFile
            );
            ImportManager importManager = SaxImportManager.create(
                    graph,
                    manager.getEntity(scopeId, PermissionScope.class),
                    getCurrentActioner(),
                    getImporterCls(importerClass, DEFAULT_EAD_IMPORTER),
                    getHandlerCls(handlerClass, DEFAULT_EAD_HANDLER),
                    options
            );
            ImportLog log = importDataStream(importManager, message, tag, data,
                    MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE);

            if (commit) {
                logger.debug("Committing EAD import transaction...");
                tx.success();
            }

            return log;
        }
    }

    /**
     * Synchronise a repository or fonds via EAD data, removing
     * items which are not present in the incoming data. Parameters
     * are the same as /ead with the addition of `ex={id}`, which
     * allows specifying items to ignore, and fondsId, which allows
     * synchronising just within a specific item.
     *
     * @param scopeId       the id of the import scope (i.e. repository)
     * @param fonds         the ID of a specific fonds within the repository
     *                      scope to synchronise. If missing the scope will be
     *                      used
     * @param ex            the ID of an item to be excluded from the sync operation
     * @param useSourceId   by default a description will update an existing description if it
     *                      has the same language code. Setting this option to 'true' allows adding
     *                      multiple descriptions in the same language by taking into account the
     *                      value of the EAD ID.
     * @param logMessage    log message for import. If this refers to an accessible local file
     *                      its contents will be used.
     * @param handlerClass  the fully-qualified handler class name (defaults to EadHandler)
     * @param importerClass the fully-qualified import class name (defaults to EadImporter)
     * @param propertyFile  a local file path or URL pointing to an import properties configuration file.
     * @param lang          the default description language, if not inferrable from the data
     * @param tag           if the data is a stream of XML a string &quot;tag&quot; can be
     *                      provided to identify the source, e.g. the name of the file from
     *                      which the stream derives. If a tag is not provided, or the
     *                      input data is not an XML stream, the default value of
     *                      &quot;-&quot; will be used
     * @param allowUpdates  allow the importer to update items that already exist. If it
     *                      attempts to do so without this option enabled an error will
     *                      be thrown
     * @param tolerant      whether or not to die on the first validation error
     * @param commit        commit the operation to the database. The default
     *                      mode is to operate as a dry-run
     * @param data          file data containing one of: a single EAD file,
     *                      multiple EAD files in an archive, a list of file URLs.
     *                      The Content-Type header is used to distinguish
     *                      the contents.
     * @return a {@link SyncLog} instance.
     * @throws ItemNotFound          if the scope does not exist
     * @throws ImportValidationError if data constraints are not met
     * @throws IOException           if an error occurs reading the input data
     * @throws DeserializationError  if the input data is not well-formed
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("ead-sync")
    public SyncLog syncEad(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @QueryParam("fonds") String fonds,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @DefaultValue("false") @QueryParam(USE_SOURCE_ID_PARAM) Boolean useSourceId,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(TAG_PARAM) @DefaultValue("-") String tag,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            @QueryParam("ex") Set<String> ex,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data)
            throws ItemNotFound, ImportValidationError, IOException, DeserializationError {

        try (final Tx tx = beginTx()) {
            checkPropertyFile(propertyFile);
            Class<? extends SaxXmlHandler> handler
                    = getHandlerCls(handlerClass, DEFAULT_EAD_HANDLER);
            Class<? extends ItemImporter<?, ?>> importer
                    = getImporterCls(importerClass, DEFAULT_EAD_IMPORTER);

            Actioner user = getCurrentActioner();
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);
            PermissionScope syncScope = fonds == null
                    ? scope
                    : manager.getEntity(fonds, PermissionScope.class);

            // Run the sync...
            String message = getLogMessage(logMessage).orElse(null);
            ImportOptions options = ImportOptions.create(tolerant, allowUpdates, useSourceId, lang, propertyFile);
            SaxImportManager importManager = SaxImportManager.create(graph, scope, user, importer, handler, options);
            // Note that while the import manager uses the scope, here
            // we use the fonds as the scope, which might be different.
            EadSync syncManager = EadSync.create(graph, api(), syncScope, user, importManager);
            SyncLog log = syncManager.sync(m -> importDataStream(m, message, tag, data,
                    MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE), ex, message);

            if (commit) {
                logger.debug("Committing EAD sync transaction...");
                tx.success();
            }
            return log;
        } catch (EadSync.EadSyncError e) {
            throw new DeserializationError(e.getMessage());
        } catch (DeserializationError e) {
            e.printStackTrace();
            throw e;
        }
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("eag")
    public ImportLog importEag(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(LANG_PARAM) String defaultLang,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(TAG_PARAM) @DefaultValue("-") String tag,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data)
            throws ItemNotFound, ImportValidationError, IOException, DeserializationError {
        try (final Tx tx = beginTx()) {
            checkPropertyFile(propertyFile);

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportOptions options = ImportOptions.create(
                    tolerant,
                    allowUpdates,
                    false,
                    defaultLang,
                    nameOrDefault(propertyFile, "eag.properties")
            );
            ImportManager importManager = SaxImportManager.create(
                    graph,
                    manager.getEntity(scopeId, PermissionScope.class),
                    getCurrentActioner(),
                    getImporterCls(importerClass, EagImporter.class.getName()),
                    getHandlerCls(handlerClass, EagHandler.class.getName()),
                    options
            );
            ImportLog log = importDataStream(importManager, message, tag, data,
                    MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE);

            if (commit) {
                logger.debug("Committing EAG import transaction...");
                tx.success();
            }

            return log;
        }
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("eac")
    public ImportLog importEac(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(LANG_PARAM) String defaultLang,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(TAG_PARAM) @DefaultValue("-") String tag,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data)
            throws ItemNotFound, ImportValidationError, IOException, DeserializationError {
        try (final Tx tx = beginTx()) {
            checkPropertyFile(propertyFile);

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportOptions options = ImportOptions.create(
                    tolerant,
                    allowUpdates,
                    false,
                    defaultLang,
                    nameOrDefault(propertyFile, "eac.properties")
            );
            ImportManager importManager = SaxImportManager.create(
                    graph,
                    manager.getEntity(scopeId, PermissionScope.class),
                    getCurrentActioner(),
                    getImporterCls(importerClass, EacImporter.class.getName()),
                    getHandlerCls(handlerClass, EacHandler.class.getName()),
                    options
            );
            ImportLog log = importDataStream(importManager, message, tag, data,
                    MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE);

            if (commit) {
                logger.debug("Committing EAC import transaction...");
                tx.success();
            }

            return log;
        }
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE,
            MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("csv")
    public ImportLog importCsv(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            @QueryParam(TAG_PARAM) @DefaultValue("-") String tag,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data)
            throws ItemNotFound, ImportValidationError, IOException, DeserializationError {
        try (final Tx tx = beginTx()) {

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportOptions options = ImportOptions.create(
                    tolerant,
                    allowUpdates,
                    false,
                    lang,
                    null
            );
            ImportManager importManager = CsvImportManager.create(
                    graph,
                    manager.getEntity(scopeId, PermissionScope.class),
                    getCurrentActioner(),
                    getImporterCls(importerClass, DEFAULT_EAD_IMPORTER),
                    options
            );
            ImportLog log = importDataStream(importManager, message, tag, data,
                    MediaType.valueOf(CSV_MEDIA_TYPE));
            if (commit) {
                logger.debug("Committing CSV import transaction...");
                tx.success();
            }
            return log;
        }
    }

    /**
     * Link items in a scope via an access point coreference table.
     * <p>
     * Each data row must consist of 2 columns:
     * <ol>
     * <li>the access point label (text string)
     * <li>the target item ID
     * </ol>
     * <p>
     * A single log item will be created for the entire CSV.
     *
     * @param scopeId  the id of the permission scope item
     * @param table    the tabular data
     * @param tolerant don't abort on the first error
     * @param commit   actually change the graph
     * @return an import log
     * @throws DeserializationError the problems are found with the import data
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("coreferences")
    public ImportLog importCoreferences(@QueryParam(SCOPE_PARAM) String scopeId,
                                        @QueryParam(TOLERANT_PARAM) @DefaultValue("false") Boolean tolerant,
                                        @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
                                        Table table) throws DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);
            ImportLog log = new LinkImporter(graph, getCurrentActioner(), tolerant)
                    .importCoreferences(scope, table, getLogMessage().orElse(null));
            if (commit) {
                logger.debug("Committing coreference import transaction...");
                tx.success();
            }
            return log;
        }
    }

    /**
     * Create multiple links via CSV or JSON tabular upload.
     * <p>
     * Each data row must consist of 6 columns:
     * </p>
     * <ol>
     * <li>the source item ID
     * <li>the target item ID
     * <li>the access point ID (optional)
     * <li>the link type, e.g. associative, hierarchical
     * <li>the link field (if applicable) e.g. relatedUnitsOfDescription
     * <li>the link description (optional)
     * </ol>
     * <p>
     * A single log item will be created for the entire CSV.
     * </p>
     *
     * @param tolerant don't abort on the first error
     * @param commit   actually change the graph
     * @param table    the tabular data
     * @return an import log
     * @throws DeserializationError the problems are found with the import data
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("links")
    public ImportLog importLinks(
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            Table table) throws DeserializationError {
        try (final Tx tx = beginTx()) {
            ImportLog log = new LinkImporter(graph, getCurrentActioner(), tolerant)
                    .importLinks(table, getLogMessage().orElse(null));
            if (commit) {
                logger.debug("Committing link import transaction...");
                tx.success();
            }
            return log;
        }
    }

    // Helpers

    private ImportLog importDataStream(
            ImportManager importManager, String message, String tag, InputStream data, MediaType... accepts)
            throws DeserializationError, ImportValidationError {
        MediaType mediaType = requestHeaders.getMediaType();
        try {
            if (MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)) {
                // Extract our list of paths...
                List<String> paths = getFilePaths(IOUtils.toString(data, StandardCharsets.UTF_8));
                return importManager.importFiles(paths, message);
            } else if (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
                return importManager.importJson(data, message);
            } else if (isCompatibleType(mediaType, accepts)) {
                return importManager.importInputStream(data, tag, message);
            } else {
                return importPotentiallyGZippedArchive(importManager, message, data);
            }
        } catch (EOFException e) {
            throw new DeserializationError("EOF reading input data");
        } catch (InputParseError | IOException e) {
            throw new DeserializationError("ParseError: " + e.getMessage());
        } catch (IllegalArgumentException | ArchiveException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    private boolean isCompatibleType(MediaType mediaType, MediaType... accepts) {
        for (MediaType acceptable : accepts) {
            if (acceptable.isCompatible(mediaType)) {
                return true;
            }
        }
        return false;
    }

    private ImportLog importPotentiallyGZippedArchive(
            ImportManager importManager, String message, InputStream data)
            throws IOException, ImportValidationError, ArchiveException, InputParseError {
        try (BufferedInputStream bufStream = new BufferedInputStream(data)) {
            bufStream.mark(0);
            try (GZIPInputStream gzipStream = new GZIPInputStream(bufStream)) {
                logger.debug("Importing gzipped archive stream...");
                return importArchive(importManager, message, gzipStream);
            } catch (java.util.zip.ZipException e) {
                // Assume data is not in zip format?
                bufStream.reset();
                logger.debug("Importing uncompressed archive stream...");
                return importArchive(importManager, message, bufStream);
            }
        }
    }

    private ImportLog importArchive(ImportManager importManager, String message, InputStream data)
            throws IOException, ImportValidationError, ArchiveException, InputParseError {
        try (BufferedInputStream bis = new BufferedInputStream(data);
             ArchiveInputStream archiveInputStream = new
                     ArchiveStreamFactory(StandardCharsets.UTF_8.displayName())
                     .createArchiveInputStream(bis)) {
            return importManager.importArchive(archiveInputStream, message);
        }
    }

    private static List<String> getFilePaths(String pathList) throws DeserializationError {
        List<String> files = Lists.newArrayList();
        for (String path : Splitter.on("\n").omitEmptyStrings().trimResults().split(pathList)) {
            if (!Files.isRegularFile(Paths.get(path))) {
                throw new DeserializationError("File specified in payload not found: " + path);
            }
            files.add(path);
        }
        return files;
    }

    private static void checkPropertyFile(String properties) throws DeserializationError {
        // Null properties are allowed
        if (properties != null) {
            java.nio.file.Path file = Paths.get(properties);
            if (!Files.isRegularFile(file)) {
                try {
                    new URL(properties);
                } catch (MalformedURLException e) {
                    throw new DeserializationError("Properties file '" + properties + "' " +
                            "either does not exist as a regular file or is an invalid URL.");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends SaxXmlHandler> getHandlerCls(String handlerName, String defaultHandler)
            throws DeserializationError {
        String name = nameOrDefault(handlerName, defaultHandler);
        try {
            Class<?> handler = Class.forName(name);
            if (!SaxXmlHandler.class.isAssignableFrom(handler)) {
                throw new DeserializationError("Class '" + handlerName + "' is" +
                        " not an instance of " + SaxXmlHandler.class.getSimpleName());
            }
            return (Class<? extends SaxXmlHandler>) handler;
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ItemImporter<?, ?>> getImporterCls(String importerName, String defaultImporter)
            throws DeserializationError {
        String name = nameOrDefault(importerName, defaultImporter);
        try {
            Class<?> importer = Class.forName(name);
            if (!ItemImporter.class.isAssignableFrom(importer)) {
                throw new DeserializationError("Class '" + importerName + "' is" +
                        " not an instance of " + ItemImporter.class.getSimpleName());
            }
            return (Class<? extends ItemImporter<?, ?>>) importer;
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        }
    }

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

    private static String nameOrDefault(String name, String defaultName) {
        return (name == null || name.trim().isEmpty()) ? defaultName : name;
    }
}
