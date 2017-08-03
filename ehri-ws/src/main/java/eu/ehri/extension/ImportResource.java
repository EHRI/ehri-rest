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
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.links.LinkImporter;
import eu.ehri.project.utils.Table;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporterFactory;
import eu.ehri.project.importers.eac.EacHandler;
import eu.ehri.project.importers.eac.EacImporter;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.eag.EagHandler;
import eu.ehri.project.importers.eag.EagImporter;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.json.BatchOperations;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.shared.NoReaderForLangException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    public static final String LOG_PARAM = "log";
    public static final String SCOPE_PARAM = "scope";
    public static final String TOLERANT_PARAM = "tolerant";
    public static final String BASE_URI_PARAM = "baseURI";
    public static final String URI_SUFFIX_PARAM = "suffix";
    public static final String ALLOW_UPDATES_PARAM = "allow-update";
    public static final String VERSION_PARAM = "version";
    public static final String HANDLER_PARAM = "handler";
    public static final String IMPORTER_PARAM = "importer";
    public static final String PROPERTIES_PARAM = "properties";
    public static final String FORMAT_PARAM = "format";

    public static final String CSV_MEDIA_TYPE = "text/csv";

    public ImportResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Import a SKOS file, of varying formats, as specified by the &quot;language&quot;
     * column of the file extensions table <a href="https://jena.apache.org/documentation/io/">here</a>.
     * <p>
     * Example:
     * <p>
     * <pre>
     *    <code>
     * curl -X POST \
     *      -H "X-User: mike" \
     *      --data-binary @skos-data.rdf \
     *      "http://localhost:7474/ehri/import/skos?scope=gb-my-vocabulary&amp;log=testing&amp;tolerant=true"
     *     </code>
     * </pre>
     *
     * @param scopeId    the id of the import scope (i.e. repository)
     * @param tolerant   whether or not to die on the first validation error
     * @param logMessage log message for import. If this refers to an accessible local file
     *                   its contents will be used.
     * @param baseURI    a URI prefix common to ingested items that will be removed
     *                   from each item's URI to obtain the local identifier.
     * @param uriSuffix  a URI suffix common to ingested items that will be removed
     *                   from each item's URI to obtain the local identifier.
     * @param format     the RDF format of the POSTed data
     * @param stream     a stream of SKOS data in a valid format.
     * @return a JSON object showing how many records were created,
     * updated, or unchanged.
     */
    @POST
    @Path("skos")
    public ImportLog importSkos(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @QueryParam(BASE_URI_PARAM) String baseURI,
            @QueryParam(URI_SUFFIX_PARAM) String uriSuffix,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(FORMAT_PARAM) String format,
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
                    .setBaseURI(baseURI)
                    .setURISuffix(uriSuffix)
                    .importFile(stream, getLogMessage(logMessage).orElse(null));
            logger.debug("Committing SKOS import transaction...");
            tx.success();
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
     * <li>multiple EAD files in an archive</li>
     * <li>a plain test file containing local file paths</li>
     * </ul>
     * The Content-Type header is used to distinguish the contents.
     * <br>
     * <b>Note:</b> The archive does not currently support compression.
     * <p>
     * The way you would run with would typically be:
     * <p>
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
     * @param tolerant      whether or not to die on the first validation error
     * @param allowUpdates  allow the importer to update items that already exist. If it
     *                      attempts to do so without this option enabled an error will
     *                      be thrown
     * @param logMessage    log message for import. If this refers to an accessible local file
     *                      its contents will be used.
     * @param handlerClass  the fully-qualified handler class name
     *                      (defaults to EadHandler)
     * @param importerClass the fully-qualified import class name
     *                      (defaults to EadImporter)
     * @param propertyFile  a local file path pointing to an import properties
     *                      configuration file.
     * @param data          file data containing one of: a single EAD file,
     *                      multiple EAD files in an archive, a list of local file
     *                      paths. The Content-Type header is used to distinguish
     *                      the contents.
     * @return a JSON object showing how many records were created,
     * updated, or unchanged.
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("ead")
    public ImportLog importEad(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream data)
            throws ItemNotFound, ValidationError, IOException, DeserializationError {

        try (final Tx tx = beginTx()) {
            checkPropertyFile(propertyFile);
            Class<? extends SaxXmlHandler> handler
                    = getHandlerCls(handlerClass, DEFAULT_EAD_HANDLER);
            Class<? extends ItemImporter> importer
                    = getImporterCls(importerClass, DEFAULT_EAD_IMPORTER);

            // Get the current user from the Authorization header and the scope
            // from the query params...
            Actioner user = getCurrentActioner();
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportManager importManager = new SaxImportManager(
                    graph, scope, user, importer, handler)
                    .allowUpdates(allowUpdates)
                    .setTolerant(tolerant)
                    .withProperties(propertyFile);
            ImportLog log = importDataStream(importManager, message, data,
                    MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE);
            logger.debug("Committing import transaction...");
            tx.success();
            return log;
        }
    }

    /**
     * Import EAG files. See EAD import for details.
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("eag")
    public ImportLog importEag(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream data)
            throws ItemNotFound, ValidationError, IOException, DeserializationError {
        return importEad(scopeId, tolerant, allowUpdates, logMessage, propertyFile,
                nameOrDefault(handlerClass, EagHandler.class.getName()),
                nameOrDefault(importerClass, EagImporter.class.getName()), data);
    }

    /**
     * Import EAC files. See EAD import for details.
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM})
    @Path("eac")
    public ImportLog importEac(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream data)
            throws ItemNotFound, ValidationError, IOException, DeserializationError {
        return importEad(scopeId, tolerant, allowUpdates, logMessage, propertyFile,
                nameOrDefault(handlerClass, EacHandler.class.getName()),
                nameOrDefault(importerClass, EacImporter.class.getName()), data);
    }

    /**
     * Import a set of CSV files. See EAD handler for options and
     * defaults but substitute text/csv for the input mimetype when
     * a single file is POSTed.
     * <p>
     * Additional note: no handler class is required.
     */
    @POST
    @Consumes({MediaType.TEXT_PLAIN, CSV_MEDIA_TYPE,
            MediaType.APPLICATION_OCTET_STREAM})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("csv")
    public ImportLog importCsv(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("false") @QueryParam(ALLOW_UPDATES_PARAM) Boolean allowUpdates,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream data)
            throws ItemNotFound, ValidationError, IOException, DeserializationError {
        try (final Tx tx = beginTx()) {
            Class<? extends ItemImporter> importer
                    = getImporterCls(importerClass, DEFAULT_EAD_IMPORTER);

            // Get the current user from the Authorization header and the scope
            // from the query params...
            Actioner user = getCurrentActioner();
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);

            // Run the import!
            String message = getLogMessage(logMessage).orElse(null);
            ImportManager importManager = new CsvImportManager(
                    graph, scope, user, tolerant, allowUpdates, importer);
            ImportLog log = importDataStream(importManager, message, data,
                    MediaType.valueOf(CSV_MEDIA_TYPE));
            logger.debug("Committing import transaction...");
            tx.success();
            return log;
        }
    }

    /**
     * Update a batch of items via JSON containing (partial)
     * data bundles.
     *
     * @param scopeId     the ID of there item's permission scope
     * @param tolerant    whether to allow individual validation failures
     * @param version     whether to create a version prior to delete
     * @param logMessage  an optional log message
     * @param inputStream a JSON document containing partial bundles containing
     *                    the needed data transformations
     * @return an import log describing the changes committed
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("batch")
    public ImportLog batchUpdate(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @DefaultValue("true") @QueryParam(VERSION_PARAM) Boolean version,
            @QueryParam(LOG_PARAM) String logMessage, InputStream inputStream)
            throws IOException, ItemNotFound, ValidationError, DeserializationError {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
            PermissionScope scope = scopeId != null
                    ? manager.getEntity(scopeId, PermissionScope.class)
                    : null;
            ImportLog log = new BatchOperations(graph, scope, version, tolerant, Collections.emptyList())
                    .batchUpdate(inputStream, user, getLogMessage(logMessage));
            logger.debug("Committing batch update transaction...");
            tx.success();
            return log;
        }
    }

    /**
     * Delete a batch of objects via JSON containing their IDs.
     *
     * @param scopeId    the ID of there item's permission scope
     * @param version    whether to create a version prior to delete
     * @param logMessage an optional log message.
     * @param ids        a list of IDs to delete
     */
    @DELETE
    @Path("batch")
    public void batchDelete(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("true") @QueryParam(VERSION_PARAM) Boolean version,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(ID_PARAM) List<String> ids)
            throws IOException, ItemNotFound, DeserializationError {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
            PermissionScope scope = scopeId != null
                    ? manager.getEntity(scopeId, PermissionScope.class)
                    : null;
            new BatchOperations(graph, scope, version, false, Collections.emptyList())
                    .batchDelete(ids, user, getLogMessage(logMessage));
            logger.debug("Committing delete transaction...");
            tx.success();
        }
    }


    /**
     * Create multiple links via CSV or JSON tabular upload.
     * <p>
     * Each data row must consist of 5 columns:
     * <ol>
     * <li>the source item
     * <li>the target item
     * <li>the link type, e.g. associative, hierarchical
     * <li>the link field (if applicable) e.g. relatedUnitsOfDescription
     * <li>the link description
     * </ol>
     * <p>
     * A separate log item will be created for each row.
     *
     * @param table the tabular data
     * @return a single column table of link IDs
     * @throws DeserializationError the problems are found with the import data
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, "text/csv"})
    @Path("links")
    public ImportLog importLinks(
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            Table table) throws DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            ImportLog log = new LinkImporter(graph, getCurrentActioner(), tolerant)
                    .importLinks(table, getLogMessage().orElse(null));
            tx.success();
            return log;
        }
    }

    // Helpers

    private ImportLog importDataStream(
            ImportManager importManager, String message, InputStream data, MediaType... accepts)
            throws DeserializationError, ValidationError {
        MediaType mediaType = requestHeaders.getMediaType();
        try {
            if (MediaType.TEXT_PLAIN_TYPE.equals(mediaType)) {
                // Extract our list of paths...
                List<String> paths = getFilePaths(IOUtils.toString(data, StandardCharsets.UTF_8));
                return importManager.importFiles(paths, message);
            } else if (Lists.newArrayList(accepts).contains(mediaType)) {
                return importManager.importInputStream(data, message);
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

    private ImportLog importPotentiallyGZippedArchive(
            ImportManager importManager, String message, InputStream data)
            throws IOException, ValidationError, ArchiveException, InputParseError {
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
            throws IOException, ValidationError, ArchiveException, InputParseError {
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
                throw new DeserializationError("Properties file '" + properties + "' " +
                        "either does not exist, or is not a file.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends SaxXmlHandler> getHandlerCls(String handlerName, String
            defaultHandler)
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
    private static Class<? extends ItemImporter> getImporterCls(String importerName, String defaultImporter)
            throws DeserializationError {
        String name = nameOrDefault(importerName, defaultImporter);
        try {
            Class<?> importer = Class.forName(name);
            if (!ItemImporter.class.isAssignableFrom(importer)) {
                throw new DeserializationError("Class '" + importerName + "' is" +
                        " not an instance of " + ItemImporter.class.getSimpleName());
            }
            return (Class<? extends ItemImporter>) importer;
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
