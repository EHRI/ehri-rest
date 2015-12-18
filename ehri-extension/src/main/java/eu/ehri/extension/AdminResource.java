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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.wrapper.AclGraph;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.tools.JsonDataExporter;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides additional Admin methods needed by client systems
 * and general graph-maintenance functionality.
 */
@Path(AdminResource.ENDPOINT)
public class AdminResource extends AbstractRestResource {

    public static final String ENDPOINT = "admin";
    public static final String DEFAULT_USER_ID_PREFIX = "user";
    public static final String DEFAULT_USER_ID_FORMAT = "%s%06d";

    public AdminResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Export the DB as a stream of JSON in
     * <a href="https://github.com/tinkerpop/blueprints/wiki/GraphSON-Reader-and-Writer-Library">GraphSON</a> format.
     * <p>
     * The mode used is EXTENDED.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_exportGraphSON")
    public Response getGraphSON() throws Exception {
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                try (final Tx tx = graph.getBaseGraph().beginTx()) {
                    Accessor accessor = getRequesterUserProfile();
                    AclGraph<?> aclGraph = new AclGraph<IndexableGraph>(graph.getBaseGraph(), accessor);
                    GraphSONWriter.outputGraph(aclGraph, stream, GraphSONMode.EXTENDED);
                    tx.success();
                }
            }
        }).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_exportJSON")
    public Response exportNodes() throws Exception {
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                try (final Tx tx = graph.getBaseGraph().beginTx()) {
                    Accessor accessor = getRequesterUserProfile();
                    AclGraph<?> aclGraph = new AclGraph<IndexableGraph>(graph.getBaseGraph(), accessor);
                    JsonDataExporter.outputGraph(aclGraph, stream);
                    tx.success();
                }
            }
        }).build();
    }

    /**
     * Re-build the graph's internal lucene index.
     * <p>
     * NB: This takes a lot of memory for large graphs. Do
     * not use willy-nilly and increase the heap size as
     * necessary. TODO: Add incremental buffered commit.
     *
     * @throws java.lang.Exception
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/_reindexInternal")
    public Response reindexInternal() throws Exception {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            manager.rebuildIndex();
            tx.success();
            return Response.ok().build();
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
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
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
            new AclManager(graph).grantPermission(user, PermissionType.OWNER, user);
            Response response = creationResponse(user);
            tx.success();
            return response;
        }
    }

    // Helpers...

    private String getNextDefaultUserId() {
        try (CloseableIterable<Vertex> query = manager.getVertices(EntityClass.USER_PROFILE)) {
            long start = Iterables.size(query) + 1;
            while (manager.exists(String.format(DEFAULT_USER_ID_FORMAT,
                    DEFAULT_USER_ID_PREFIX, start))) start++;
            return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX, start);
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
