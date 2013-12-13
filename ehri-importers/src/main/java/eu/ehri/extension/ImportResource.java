package eu.ehri.extension;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.*;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Resource class for import endpoints.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("import")
public class ImportResource extends AbstractRestResource {
    public ImportResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Import a set of EAD files. The body of the POST
     * request should be a newline separated list of file
     * paths.
     *
     * The way you would run with would typically be:
     *
     * curl -X POST \
     *          -H "Authorization: mike" \
     *          -H "Content-type: text/plain" \
     *          --data-binary @wl-list.txt \
     *          "http://localhost:7474/ehri/import/ead?scope=gb-003348&log=testing&tolerant=true"
     *
     * (Assuming wl-list.txt is a list of newline separated EAD file paths.)
     *
     * (NB: Data is sent using --data-binary to preserve linebreaks - otherwise
     * it needs url encoding.)
     *
     * (NB2: Might be better to use a different way of encoding the local
     * file paths...)
     *
     */
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/ead")
    public Response importEad(
            @QueryParam("scope") String scopeId,
            @QueryParam("tolerant") Boolean tolerant,
            @QueryParam("log") String logMessage,
            String pathList)
            throws BadRequester, ItemNotFound, ValidationError, IOException {

        Class<? extends SaxXmlHandler > handler = IcaAtomEadHandler.class;
        Class<? extends AbstractImporter> importer = IcaAtomEadImporter.class;

        // Get the current user from the Authorization header and the scope
        // from the query params...
        UserProfile user = getCurrentUser();
        PermissionScope scope = manager.getFrame(scopeId, PermissionScope.class);

        // Extract our list of paths...
        List<String> paths = getFilePaths(pathList);

        try {
            // Run the import!
            ImportLog log = new SaxImportManager(graph, scope, user, importer, handler)
                    .setTolerant(tolerant)
                    .importFiles(paths, logMessage);

            graph.getBaseGraph().commit();
            return Response.ok(logToText(log).getBytes()).build();
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
    private List<String> getFilePaths(String pathList) {
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
     * Format an import log.
     */
    private String logToText(ImportLog log) {
        return String.format("Created: %s\nUpdated: %s\nUnchanged: %s\n",
                log.getCreated(), log.getUpdated(), log.getUnchanged());
    }
}
