package eu.ehri.extension;

// Borrowed, temporarily, from Michael Hunger:
// https://github.com/jexp/neo4j-clean-remote-db-addon

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.*;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides additional Admin methods needed by client systems.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("admin")
public class AdminResource extends AbstractRestResource {

    private static ObjectMapper mapper = new ObjectMapper();
    public static String DEFAULT_USER_ID_PREFIX = "user";
    public static String DEFAULT_USER_ID_FORMAT = "%s%06d";

    public AdminResource(@Context GraphDatabaseService database) {
        super(database);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_rebuildChildCache")
    public Response rebuildChildCache() throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            for (DocumentaryUnit unit: manager.getFrames(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class)) {
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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_findReplaceProperty")
    public Response renamePropertyValue(
            @QueryParam("type") String entityType,
            @QueryParam("name") String propName,
            @QueryParam("from") String oldValue,
            @QueryParam("to") String newValue) throws Exception {
        graph.getBaseGraph().checkNotInTransaction();
        EntityClass entityClass = EntityClass.withName(entityType);
        CloseableIterable<Vertex> vertices = manager.getVertices(entityClass);
        if (propName == null
                || propName.trim().isEmpty()
                || propName.equals(EntityType.ID_KEY)
                || propName.equals(EntityType.TYPE_KEY)) {
            throw new IllegalArgumentException("Invalid propery key: " + propName);
        }
        try {
            int changes = 0;
            for (Vertex v : vertices) {
                Object current = v.getProperty(propName);
                if (oldValue.equals(current)) {
                    changes++;
                    manager.setProperty(v, propName, newValue);
                }
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK)
                    .entity(Integer.valueOf(changes).toString().getBytes())
                    .build();
        } finally {
            vertices.close();
            cleanupTransaction();
        }
    }

    /**
     * Create a new user with a default name and identifier.
     * 
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
            for (String groupId: groups) {
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
        // FIXME: It's crappy to have to iterate all the items to count them...
        long userCount = 0;
        CloseableIterable<Vertex> query = manager
                .getVertices(EntityClass.USER_PROFILE);
        try {
            for (@SuppressWarnings("unused")
            Vertex _ : query)
                userCount++;
        } finally {
            query.close();
        }
        long start = userCount + 1;
        while (manager.exists(String.format(DEFAULT_USER_ID_FORMAT,
                DEFAULT_USER_ID_PREFIX, start)))
            start++;
        return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX,
                start);
    }

    private Map<String,Object> parseUserData(String json) throws IOException {
        if (json == null || json.trim().equals("")) {
            return Maps.newHashMap();
        } else {
            TypeReference<HashMap<String,Object>> typeRef = new TypeReference<
                                    HashMap<String,Object>
                                    >() {};
            return mapper.readValue(json, typeRef);
        }
    }
}
