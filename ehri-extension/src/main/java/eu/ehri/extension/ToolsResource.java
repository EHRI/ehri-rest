package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.tools.FindReplace;
import eu.ehri.project.tools.Linker;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Miscellaneous additional functionality that doesn't
 * fit anywhere else.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(ToolsResource.ENDPOINT)
public class ToolsResource extends AbstractRestResource {

    private final FindReplace findReplace;
    private final Linker linker;

    public static final String ENDPOINT = "tools";
    public static final String TOLERANT_PARAM = "tolerant";
    public static final String SINGLE_PARAM = "single";
    public static final String LANG_PARAM = "lang";
    public static final String ACCESS_POINT_TYPE_PARAM = "apt";
    public static final String DEFAULT_LANG = "eng";

    public ToolsResource(@Context GraphDatabaseService database) {
        super(database);
        findReplace = new FindReplace(graph);
        linker = new Linker(graph);
    }

    /**
     * Create concepts and links derived from the access points
     * on a repository's documentary units.
     *
     * @param repositoryId     the repository id
     * @param vocabularyId     the target vocabulary
     * @param languageCode     the language code of created concepts
     * @param accessPointTypes the access point types to process
     * @param tolerant         proceed even if there are integrity errors due
     *                         to slug collisions in the created concepts
     * @param excludeSingle    don't create concepts/links for access points that
     *                         are unique to a single item
     * @return the number of links created
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateConcepts/{repositoryId:.+}/{vocabularyId:.+}")
    public long autoLinkRepositoryDocs(
            @PathParam("repositoryId") String repositoryId,
            @PathParam("vocabularyId") String vocabularyId,
            @QueryParam(ACCESS_POINT_TYPE_PARAM) List<String> accessPointTypes,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String languageCode,
            @QueryParam(SINGLE_PARAM) @DefaultValue("true") boolean excludeSingle,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant)
            throws ItemNotFound, BadRequester, ValidationError,
            PermissionDenied, DeserializationError {
        graph.getBaseGraph().checkNotInTransaction();

        try {
            UserProfile user = getCurrentUser();
            Repository repository = manager.getFrame(repositoryId, Repository.class);
            Vocabulary vocabulary = manager.getFrame(vocabularyId, Vocabulary.class);

            long linkCount = linker
                    .withAccessPointTypes(accessPointTypes)
                    .withTolerant(tolerant)
                    .withExcludeSingles(excludeSingle)
                    .withDefaultLanguage(languageCode)
                    .withLogMessage(getLogMessage())
                    .createAndLinkRepositoryVocabulary(repository, vocabulary, user);

            graph.getBaseGraph().commit();
            return linkCount;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Find an replace a regex-specified substring of a property value
     * across an entire entity class e.g.
     * if an Address has a property with name &quot;url&quot; and value &quot;www.foo.com/bar&quot;,
     * providing a regex value <code>^www</code> and replacement <code>http://www</code> will
     * give the property a value of &quot;http://www.foo.com/bar&quot;.
     * <p/>
     * <strong>Warning: This is a sharp tool! Back up the whole database first!</strong>
     *
     * @param entityType The type of entity
     * @param propName   The name of the property to find and replace
     * @param regex      A regex specifying a substring of the property value
     * @param replace    A replacement substring
     * @return How many items have been changed
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_findReplacePropertyValueRE")
    public long renamePropertyValueRE(
            @QueryParam("type") String entityType,
            @QueryParam("name") String propName,
            @QueryParam("pattern") String regex,
            @QueryParam("replace") String replace) throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        EntityClass entityClass = EntityClass.withName(entityType);
        Pattern pattern = Pattern.compile(regex);
        try {
            long changes = findReplace
                    .propertyValueRE(entityClass, propName, pattern, replace);
            graph.getBaseGraph().commit();
            return changes;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Change a property key name across an entire entity class.
     * <p/>
     * <strong>Warning: This is a sharp tool! Back up the whole database first!</strong>
     *
     * @param entityType The type of entity
     * @param oldKeyName The existing property key name
     * @param newKeyName The new property key name
     * @return The number of items changed
     * @throws Exception
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_findReplacePropertyName")
    public long renamePropertyName(
            @QueryParam("type") String entityType,
            @QueryParam("from") String oldKeyName,
            @QueryParam("to") String newKeyName) throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        EntityClass entityClass = EntityClass.withName(entityType);
        try {
            long changes = findReplace
                    .propertyName(entityClass, oldKeyName, newKeyName);
            graph.getBaseGraph().commit();
            return changes;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Find an replace a property value across an entire entity class, e.g.
     * if a DocumentaryUnit has a property with name &quot;foo&quot; and value &quot;bar&quot;,
     * change the value to &quot;baz&quot; on all items.
     * <p/>
     * <strong>Warning: This is a sharp tool! Back up the whole database first!</strong>
     *
     * @param entityType The type of entity
     * @param propName   The name of the property to find and replace
     * @param oldValue   The property value to change
     * @param newValue   The new value
     * @return How many items have been changed
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_findReplacePropertyValue")
    public long renamePropertyValue(
            @QueryParam("type") String entityType,
            @QueryParam("name") String propName,
            @QueryParam("from") String oldValue,
            @QueryParam("to") String newValue) throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        EntityClass entityClass = EntityClass.withName(entityType);
        try {
            long changes = findReplace
                    .propertyValue(entityClass, propName, oldValue, newValue);
            graph.getBaseGraph().commit();
            return changes;
        } finally {
            cleanupTransaction();
        }
    }
}
