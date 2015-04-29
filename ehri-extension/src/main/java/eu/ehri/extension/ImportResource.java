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

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.shared.NoReaderForLangException;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.CsvImportManager;
import eu.ehri.project.importers.EadHandler;
import eu.ehri.project.importers.EadImporter;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporterFactory;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.core.Tx;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Resource class for import endpoints.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("import")
public class ImportResource extends AbstractRestResource {

    private static final Class<? extends SaxXmlHandler> DEFAULT_EAD_HANDLER
            = EadHandler.class;
    private static final Class<? extends AbstractImporter> DEFAULT_EAD_IMPORTER
            = EadImporter.class;

    public static final String LOG_PARAM = "log";
    public static final String SCOPE_PARAM = "scope";
    public static final String TOLERANT_PARAM = "tolerant";
    public static final String HANDLER_PARAM = "handler";
    public static final String IMPORTER_PARAM = "importer";
    public static final String PROPERTIES_PARAM = "properties";
    public static final String FORMAT_PARAM = "format";

    public ImportResource(@Context GraphDatabaseService database) {
        super(database);
    }


    /**
     * Import a SKOS file, of varying formats, as specified by the &quot;language&quot;
     * column of the file extensions table <a href="https://jena.apache.org/documentation/io/">here</a>.
     * <p/>
     * Example:
     * <p/>
     * <pre>
     * <code>curl -X POST \
     *      -H "Authorization: mike" \
     *      --data-binary @skos-data.rdf \
     *      "http://localhost:7474/ehri/import/skos?scope=gb-my-vocabulary&log=testing&tolerant=true"
     * </code>
     * </pre>
     *
     * @param scopeId    The id of the import scope (i.e. repository)
     * @param tolerant   Whether or not to die on the first validation error
     * @param logMessage Log message for import. If this refers to an accessible local file
     *                   its contents will be used.
     * @param format     The RDF format of the POSTed data
     * @param stream     A stream of SKOS data in a valid format.
     * @return A JSON object showing how many records were created,
     *         updated, or unchanged.
     */
    @POST
//    @Consumes({"application/rdf+xml","text/turtle","application/n-triples","application/trig","application/n-quads","application/ld+json"})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/skos")
    public Response importSkos(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(FORMAT_PARAM) String format,
            InputStream stream)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            // Get the current user from the Authorization header and the scope
            // from the query params...
            UserProfile user = getCurrentUser();
            Vocabulary scope = manager.getFrame(scopeId, Vocabulary.class);
            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, user, scope);

            ImportLog log = importer
                    .setFormat(format)
                    .setTolerant(tolerant)
                    .importFile(stream, getLogMessage(logMessage).orNull());

            tx.success();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (InputParseError e) {
            throw new DeserializationError("Unable to parse input: " + e.getMessage());
        } catch (NoReaderForLangException e) {
            throw new DeserializationError("Unable to read language: " + format);
        }
    }

    /**
     * Import a set of EAD files. The body of the POST
     * request should be a newline separated list of file
     * paths.
     * <p/>
     * The way you would run with would typically be:
     * <p/>
     * <pre>
     * <code>curl -X POST \
     *      -H "Authorization: mike" \
     *      --data-binary @ead-list.txt \
     *      "http://localhost:7474/ehri/import/ead?scope=my-repo-id&log=testing&tolerant=true"
     *
     * # NB: Data is sent using --data-binary to preserve line-breaks - otherwise
     * # it needs url encoding.
     * </code>
     * </pre>
     * <p/>
     * (Assuming <code>ead-list.txt</code> is a list of newline separated EAD file paths.)
     * <p/>
     * (TODO: Might be better to use a different way of encoding the local file paths...)
     *
     * @param scopeId       The id of the import scope (i.e. repository)
     * @param tolerant      Whether or not to die on the first validation error
     * @param logMessage    Log message for import. If this refers to an accessible local file
     *                      its contents will be used.
     * @param handlerClass  The fully-qualified handler class name
     *                      (defaults to EadHandler)
     * @param importerClass The fully-qualified import class name
     *                      (defaults to EadImporter)
     * @param propertyFile  A local file path pointing to an import properties
     *                      configuration file.
     * @param pathList      A string containing a list of local file paths
     *                      to import.
     * @return A JSON object showing how many records were created,
     *         updated, or unchanged.
     */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ead")
    public Response importEad(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            String pathList)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            checkPropertyFile(propertyFile);
            Class<? extends SaxXmlHandler> handler = getEadHandler(handlerClass);
            Class<? extends AbstractImporter> importer = getEadImporter(importerClass);

            // Get the current user from the Authorization header and the scope
            // from the query params...
            UserProfile user = getCurrentUser();
            PermissionScope scope = manager.getFrame(scopeId, PermissionScope.class);

            // Extract our list of paths...
            List<String> paths = getFilePaths(pathList);

            // Run the import!
            ImportLog log = new SaxImportManager(graph, scope, user, importer, handler)
                    .withProperties(propertyFile)
                    .setTolerant(tolerant)
                    .importFiles(paths, getLogMessage(logMessage).orNull());

            tx.success();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    /**
     * Import a single EAD file. The body of the POST
     * request should be an EAD file.
     * <p/>
     * The way you would run with would typically be:
     * <p/>
     * <pre>
     * <code>curl -X POST \
     *      -H "Authorization: mike" \
     *      --data-binary @ead-file.xml \
     *      "http://localhost:7474/ehri/import/single_ead?scope=my-repo-id&log=testing&tolerant=true"
     *
     * # NB: Data is sent using --data-binary to preserve line-breaks - otherwise
     * # it needs url encoding.
     * </code>
     * </pre>
     * <p/>
     *
     * @param scopeId       The id of the import scope (i.e. repository)
     * @param tolerant      Whether or not to die on the first validation error
     * @param logMessage    Log message for import. If this refers to an accessible local file
     *                      its contents will be used.
     * @param handlerClass  The fully-qualified handler class name
     *                      (defaults to EadHandler)
     * @param importerClass The fully-qualified import class name
     *                      (defaults to EadImporter)
     * @param propertyFile  A local file path pointing to an import properties
     *                      configuration file.
     * @param input         An XML document that is a valid EAD document.
     * @return A JSON object showing how many records were created,
     *         updated, or unchanged.
     */
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/single_ead")
    public Response importSingleEad(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @DefaultValue("false") @QueryParam(TOLERANT_PARAM) Boolean tolerant,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(PROPERTIES_PARAM) String propertyFile,
            @QueryParam(HANDLER_PARAM) String handlerClass,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream input)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            checkPropertyFile(propertyFile);
            Class<? extends SaxXmlHandler> handler = getEadHandler(handlerClass);
            Class<? extends AbstractImporter> importer = getEadImporter(importerClass);

            // Get the current user from the Authorization header and the scope
            // from the query params...
            UserProfile user = getCurrentUser();
            PermissionScope scope = manager.getFrame(scopeId, PermissionScope.class);

            // Run the import!
            ImportLog log = new SaxImportManager(graph, scope, user, importer, handler)
                    .withProperties(propertyFile)
                    .setTolerant(tolerant)
                    .importFile(input, getLogMessage(logMessage).orNull());

            tx.success();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        } catch (IllegalArgumentException | InputParseError e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    /**
     * Import a set of CSV files. The body of the POST
     * request should be a newline separated list of file
     * paths.
     * <p/>
     * The way you would run with would typically be:
     * <p/>
     * <pre>
     * <code>curl -X POST \
     *      -H "Authorization: mike" \
     *      --data-binary @csv-list.txt \
     *      "http://localhost:7474/ehri/import/csv?scope=my-repo-id&log=testing"
     *
     * # NB: Data is sent using --data-binary to preserve line-breaks - otherwise
     * # it needs url encoding.
     * </code>
     * </pre>
     * <p/>
     * (Assuming <code>csv-list.txt</code> is a list of newline separated CSV file paths.)
     * <p/>
     * (TODO: Might be better to use a different way of encoding the local file paths...)
     *
     * @param scopeId       The id of the import scope (i.e. repository)
     * @param logMessage    Log message for import. If this refers to a local file
     *                      its contents will be used.
     * @param importerClass The fully-qualified import class name
     * @param stream        A stream of CSV data
     *
     * There is no property file for this. Either the csv-heading is already in graph-compatible wording, or the Importer takes care of this.
     * @return A JSON object showing how many records were created,
     *         updated, or unchanged.
     */
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/csv")
    public Response importCsv(
            @QueryParam(SCOPE_PARAM) String scopeId,
            @QueryParam(LOG_PARAM) String logMessage,
            @QueryParam(IMPORTER_PARAM) String importerClass,
            InputStream stream)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Class<? extends AbstractImporter> importer = getEadImporter(importerClass);

            // Get the current user from the Authorization header and the scope
            // from the query params...
            UserProfile user = getCurrentUser();
            PermissionScope scope = manager.getFrame(scopeId, PermissionScope.class);

            // Run the import!
            ImportLog log = new CsvImportManager(graph, scope, user, importer)
                    .importFile(stream, getLogMessage(logMessage).orNull());

            tx.success();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (InputParseError ex) {
            throw new DeserializationError("ParseError: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    /**
     * Extract and validate input path list.
     *
     * @param pathList Newline separated list of file paths.
     * @return Validated list of path strings.
     */
    private static List<String> getFilePaths(String pathList) {
        List<String> files = Lists.newArrayList();
        for (String path : Splitter.on("\n").omitEmptyStrings().trimResults().split(pathList)) {
            if (!new File(path).exists()) {
                throw new IllegalArgumentException("File specified in payload not found: " + path);
            }
            files.add(path);
        }
        return files;
    }

    private static void checkPropertyFile(String properties) {
        // Null properties are allowed
        if (properties != null) {
            File file = new File(properties);
            if (!(file.isFile() && file.exists())) {
                throw new IllegalArgumentException("Properties file '" + properties + "' " +
                        "either does not exist, or is not a file.");
            }
        }
    }

    /**
     * Load a handler by name. Note: a valid class that's not a valid handler
     * will throw a `NoSuchMethodException` in the import manager but not actually
     * crash, so the import will appear to do nothing.
     *
     * @param handlerName The handler name
     * @return A handler class
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends SaxXmlHandler> getEadHandler(String handlerName)
            throws ClassNotFoundException, DeserializationError {
        if (handlerName == null || handlerName.trim().isEmpty()) {
            return DEFAULT_EAD_HANDLER;
        } else {
            Class<?> handler = Class.forName(handlerName);
            if (!SaxXmlHandler.class.isAssignableFrom(handler)) {
                throw new DeserializationError("Class '" + handlerName + "' is" +
                        " not an instance of " + SaxXmlHandler.class.getSimpleName());
            }
            return (Class<? extends SaxXmlHandler>) handler;
        }
    }

    /**
     * Load an importer by name. Note: a valid class that's not a valid importer
     * will throw a `NoSuchMethodException` in the import manager but not actually
     * crash, so the import will appear to do nothing.
     *
     * @param importerName The importer name
     * @return An importer class
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractImporter> getEadImporter(String importerName)
            throws ClassNotFoundException, DeserializationError {
        if (importerName == null || importerName.trim().isEmpty()) {
            return DEFAULT_EAD_IMPORTER;
        } else {
            Class<?> importer = Class.forName(importerName);
            if (!AbstractImporter.class.isAssignableFrom(importer)) {
                throw new DeserializationError("Class '" + importerName + "' is" +
                        " not an instance of " + AbstractImporter.class.getSimpleName());
            }
            return (Class<? extends AbstractImporter>) importer;
        }
    }

    private Optional<String> getLogMessage(String logMessage) throws IOException {
        if (logMessage == null || logMessage.isEmpty()) {
            return Optional.absent();
        } else {
            File fileTest = new File(logMessage);
            if (fileTest.exists()) {
                return Optional.of(FileUtils.readFileToString(fileTest, "UTF-8"));
            } else {
                return Optional.of(logMessage);
            }
        }
    }
}
