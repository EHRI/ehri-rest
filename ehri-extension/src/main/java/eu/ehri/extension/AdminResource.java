package eu.ehri.extension;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.Utilities;
import eu.ehri.project.views.ViewFactory;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provides additional Admin methods needed by client systems
 * and general graph-maintenance functionality.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("admin")
public class AdminResource extends AbstractRestResource {

    public static final String DEFAULT_USER_ID_PREFIX = "user";
    public static final String DEFAULT_USER_ID_FORMAT = "%s%06d";

    private final Utilities utilViews;

    public AdminResource(@Context GraphDatabaseService database) {
        super(database);
        utilViews = new Utilities(graph);
    }

    /**
     * Update the childCount property on hierarchical items with the number of
     * children they contain.
     *
     * @throws Exception
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_rebuildChildCache")
    public Response rebuildChildCache() throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            for (DocumentaryUnit unit : manager.getFrames(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class)) {
                unit.updateChildCountCache();
            }
            for (Repository repository : manager.getFrames(EntityClass.REPOSITORY, Repository.class)) {
                repository.updateChildCountCache();
            }
            for (Country country : manager.getFrames(EntityClass.COUNTRY, Country.class)) {
                country.updateChildCountCache();
            }
            for (Group group : manager.getFrames(EntityClass.GROUP, Group.class)) {
                group.updateChildCountCache();
            }
            for (Concept concept : manager.getFrames(EntityClass.CVOC_CONCEPT, Concept.class)) {
                concept.updateChildCountCache();
            }
            for (Vocabulary vocabulary : manager.getFrames(EntityClass.CVOC_VOCABULARY, Vocabulary.class)) {
                vocabulary.updateChildCountCache();
            }
            for (AuthoritativeSet set : manager.getFrames(EntityClass.AUTHORITATIVE_SET, AuthoritativeSet.class)) {
                set.updateChildCountCache();
            }
            for (VirtualUnit vu : manager.getFrames(EntityClass.VIRTUAL_UNIT, VirtualUnit.class)) {
                vu.updateChildCountCache();
            }

            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Export the DB as a stream of JSON in
     * <a href="https://github.com/tinkerpop/blueprints/wiki/GraphSON-Reader-and-Writer-Library">GraphSON</a> format.
     * <p/>
     * The mode used is EXTENDED.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_exportGraphSON")
    public Response getGraphSON() throws Exception {
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                GraphSONWriter.outputGraph(graph, stream, GraphSONMode.EXTENDED);
            }
        }).build();
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
            long changes = utilViews
                    .findReplacePropertyValue(entityClass, propName, oldValue, newValue);
            graph.getBaseGraph().commit();
            return changes;
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
            long changes = utilViews
                    .findReplacePropertyValueRE(entityClass, propName, pattern, replace);
            graph.getBaseGraph().commit();
            return changes;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Change a property key name across an entire entity class.
     *
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
            long changes = utilViews
                    .replacePropertyName(entityClass, oldKeyName, newKeyName);
            graph.getBaseGraph().commit();
            return changes;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create a new user with a default name and identifier.
     *
     * @param jsonData Additional key/value data for the created object
     * @param groups   IDs for groups to which the user should belong
     * @return A new user
     * @throws Exception
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/createDefaultUserProfile")
    public Response createDefaultUserProfile(String jsonData,
                                             @QueryParam(GROUP_PARAM) List<String> groups) throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            String ident = getNextDefaultUserId();
            Bundle bundle = Bundle.Builder.withClass(EntityClass.USER_PROFILE)
                    .addDataValue(Ontology.IDENTIFIER_KEY, ident)
                    .addDataValue(Ontology.NAME_KEY, ident)
                    .addData(parseUserData(jsonData))
                    .build();

            // NB: This assumes that admin's ID is the same as its identifier.
            Accessor accessor = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                    Accessor.class);
            Crud<UserProfile> view = ViewFactory.getCrudWithLogging(graph, UserProfile.class);
            UserProfile user = view.create(bundle, accessor);

            // add to the groups
            for (String groupId : groups) {
                Group group = manager.getFrame(groupId, EntityClass.GROUP, Group.class);
                group.addMember(user);
            }

            // Grant them owner permissions on their own account.
            new AclManager(graph).grantPermission(user, PermissionType.OWNER, user
            );
            graph.getBaseGraph().commit();
            return creationResponse(user);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers...

    private String getNextDefaultUserId() {
        CloseableIterable<Vertex> query = manager.getVertices(EntityClass.USER_PROFILE);
        try {
            long start = Iterables.size(query) + 1;
            while (manager.exists(String.format(DEFAULT_USER_ID_FORMAT,
                    DEFAULT_USER_ID_PREFIX, start))) start++;
            return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX, start);
        } finally {
            query.close();
        }
    }

    private Map<String, Object> parseUserData(String json) throws IOException {
        if (json == null || json.trim().equals("")) {
            return Maps.newHashMap();
        } else {
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<
                    HashMap<String, Object>
                    >() {
            };
            return jsonMapper.readValue(json, typeRef);
        }
    }
}
