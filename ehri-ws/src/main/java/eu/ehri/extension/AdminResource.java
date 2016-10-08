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
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import eu.ehri.extension.base.AbstractResource;
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
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides additional Admin methods needed by client systems
 * and general graph-maintenance functionality.
 */
@Path(AdminResource.ENDPOINT)
public class AdminResource extends AbstractResource {

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
    @Path("export-graphson")
    public Response getGraphSON() throws Exception {
        return Response.ok((StreamingOutput) stream -> {
            try (final Tx tx = beginTx()) {
                Accessor accessor = getRequesterUserProfile();
                AclGraph<?> aclGraph = new AclGraph<Graph>(graph.getBaseGraph(), accessor);
                GraphSONWriter.outputGraph(aclGraph, stream, GraphSONMode.EXTENDED);
                tx.success();
            }
        }).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("export-json")
    public Response exportNodes() throws Exception {
        return Response.ok((StreamingOutput) stream -> {
            try (final Tx tx = beginTx()) {
                Accessor accessor = getRequesterUserProfile();
                AclGraph<?> aclGraph = new AclGraph<Graph>(graph.getBaseGraph(), accessor);
                JsonDataExporter.outputGraph(aclGraph, stream);
                tx.success();
            }
        }).build();
    }

    /**
     * Create a new user with a default name and identifier.
     *
     * @param jsonData Additional key/value data for the created object
     * @param groups   IDs for groups to which the user should belong
     * @return A new user
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("create-default-user-profile")
    public Response createDefaultUserProfile(String jsonData,
            @QueryParam(GROUP_PARAM) List<String> groups) throws Exception {
        try (final Tx tx = beginTx()) {
            String ident = getNextDefaultUserId();
            Bundle bundle = Bundle.Builder.withClass(EntityClass.USER_PROFILE)
                    .addDataValue(Ontology.IDENTIFIER_KEY, ident)
                    .addDataValue(Ontology.NAME_KEY, ident)
                    .addData(parseUserData(jsonData))
                    .build();

            // NB: This assumes that admin's ID is the same as its identifier.
            Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
            UserProfile user = api().withAccessor(admin).create(bundle, UserProfile.class);

            // add to the groups
            for (String groupId : groups) {
                Group group = manager.getEntity(groupId, EntityClass.GROUP, Group.class);
                group.addMember(user);
            }

            // Grant them owner permissions on their own account.
            api().aclManager().grantPermission(user, PermissionType.OWNER, user);
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
