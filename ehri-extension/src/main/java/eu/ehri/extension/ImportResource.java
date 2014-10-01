package eu.ehri.extension;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.shared.NoReaderForLangException;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.*;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporterFactory;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
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
            = IcaAtomEadHandler.class;
    private static final Class<? extends AbstractImporter> DEFAULT_EAD_IMPORTER
            = IcaAtomEadImporter.class;

    public ImportResource(@Context GraphDatabaseService database) {
        super(database);
    }


    /**
     * Import a SKOS file.
     * <p/>
     * Example:
     * <p/>
     * <pre>
     * <code>
     * curl -X POST \
     *      -H "Authorization: mike" \
     *      -H "Content-type: text/plain" \
     *      --data-binary @skos-data.rdf \
     *      "http://localhost:7474/ehri/import/skos?scope=gb-my-vocabulary&log=testing&tolerant=true"
     * </code>
     * </pre>
     * <p/>
     * (TODO: Might be better to use a different way of encoding the local file paths...)
     *
     * @param scopeId    The id of the import scope (i.e. repository)
     * @param tolerant   Whether or not to die on the first validation error
     * @param logMessage Log message for import
     * @param stream     A stream of SKOS data in a valid format.
     * @return A JSON structure showing how many records were created,
     *         updated, or unchanged.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/skos")
    public Response importSkos(
            @QueryParam("scope") String scopeId,
            @DefaultValue("false") @QueryParam("tolerant") Boolean tolerant,
            @QueryParam("log") String logMessage,
            @QueryParam("format") String format,
            InputStream stream)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try {
            // Get the current user from the Authorization header and the scope
            // from the query params...
            UserProfile user = getCurrentUser();
            Vocabulary scope = manager.getFrame(scopeId, Vocabulary.class);
            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, user, scope);

            ImportLog log = importer
                    .setFormat(format)
                    .setTolerant(tolerant)
                    .importFile(stream, logMessage);

            graph.getBaseGraph().commit();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (InputParseError e) {
            throw new DeserializationError("Unable to parse input: " + e.getMessage());
        } catch (NoReaderForLangException e) {
            throw new DeserializationError("Unable to read language: " + format);
        } finally {
            if (graph.getBaseGraph().isInTransaction()) {
                graph.getBaseGraph().rollback();
            }
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
     * <code>
     * curl -X POST \
     *      -H "Authorization: mike" \
     *      -H "Content-type: text/plain" \
     *      --data-binary @wl-list.txt \
     *      "http://localhost:7474/ehri/import/ead?scope=gb-003348&log=testing&tolerant=true"
     * </code>
     * </pre>
     * <p/>
     * (Assuming wl-list.txt is a list of newline separated EAD file paths.)
     * <p/>
     * (NB: Data is sent using --data-binary to preserve linebreaks - otherwise
     * it needs url encoding.)
     * <p/>
     * (TODO: Might be better to use a different way of encoding the local file paths...)
     *
     * @param scopeId       The id of the import scope (i.e. repository)
     * @param tolerant      Whether or not to die on the first validation error
     * @param logMessage    Log message for import
     * @param handlerClass  The fully-qualified handler class name
     *                      (defaults to IcaAtomEadHandler)
     * @param importerClass The fully-qualified import class name
     *                      (defaults to IcaAtomEadImporter)
     * @param pathList      A string containing a list of local file paths
     *                      to import.
     * @return A JSON structure showing how many records were created,
     *         updated, or unchanged.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/ead")
    public Response importEad(
            @QueryParam("scope") String scopeId,
            @QueryParam("tolerant") Boolean tolerant,
            @QueryParam("log") String logMessage,
            @QueryParam("handler") String handlerClass,
            @QueryParam("importer") String importerClass,
            String pathList)
            throws BadRequester, ItemNotFound, ValidationError,
            IOException, DeserializationError {

        try {
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
                    .setTolerant(tolerant)
                    .importFiles(paths, logMessage);

            graph.getBaseGraph().commit();
            return Response.ok(jsonMapper.writeValueAsBytes(log.getData())).build();
        } catch (ClassNotFoundException e) {
            throw new DeserializationError("Class not found: " + e.getMessage());
        } finally {
            if (graph.getBaseGraph().isInTransaction()) {
                graph.getBaseGraph().rollback();
            }
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
                throw new IllegalArgumentException("File not found: " + path);
            }
            files.add(path);
        }
        return files;
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
}
