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

import com.google.common.collect.Lists;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.views.GenericViews;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Provides a means of fetching items and lists of items
 * regardless of their specific type.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(GenericResource.ENDPOINT)
public class GenericResource extends AbstractAccessibleEntityResource<AccessibleEntity> {

    public static final String ENDPOINT = "entities";

    public static final String STRICT_PARAM = "strict";
    public static final String ID_PARAM = "id";
    public static final String GID_PARAM = "gid";

    private final GenericViews genericViews;

    public GenericResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
        genericViews = new GenericViews(graph);

    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param ids    a list of string IDs
     * @param strict error if any of the items does not exist
     * @return A serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response list(
            @QueryParam(ID_PARAM) List<String> ids,
            @QueryParam(STRICT_PARAM) @DefaultValue("false") boolean strict)
            throws ItemNotFound, PermissionDenied, BadRequester {
        return streamingVertexList(
                genericViews.list(ids, getRequesterUserProfile(), strict), getSerializer());
    }

    /**
     * Fetch a list of items by their ID.
     *
     * @param json   a JSON-encoded list of string IDs
     * @param strict error if any of the items does not exist
     * @return a serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listFromeJson(String json,
            @QueryParam(STRICT_PARAM) @DefaultValue("false") boolean strict)
            throws ItemNotFound, PermissionDenied, BadRequester, DeserializationError, IOException {
        return list(parseIds(json), strict);
    }

    /**
     * Fetch a list of items by their internal graph ID (gid), which is
     * provided in the bundle metadata section of the default response.
     *
     * @param gids   a list of graph IDs
     * @param strict error if any of the items does not exist
     * @return a serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public Response listByGid(@QueryParam(GID_PARAM) List<Long> gids,
            @QueryParam(STRICT_PARAM) @DefaultValue("false") boolean strict) throws ItemNotFound,
            PermissionDenied, BadRequester {

        return streamingVertexList(genericViews
                .listByGid(Lists.<Object>newArrayList(gids),
                        getRequesterUserProfile(), strict), getSerializer());
    }

    /**
     * Fetch a list of items by their internal graph ID (gid), which is
     * provided in the bundle metadata section of the default response.
     *
     * @param json   a JSON-encoded list of graph IDs
     * @param strict error if any of the items does not exist
     * @return a serialized list of items
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/listByGraphId")
    public Response listByGidFromJson(String json,
            @QueryParam(STRICT_PARAM) @DefaultValue("false") boolean strict) throws ItemNotFound,
            PermissionDenied, DeserializationError, BadRequester, IOException {

        return listByGid(parseGraphIds(json), strict);
    }

    /**
     * Fetch an item of any type by ID.
     *
     * @param id The item's ID
     * @return A serialized representation.
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/{id:.+}")
    public Response get(@PathParam(ID_PARAM) String id) throws ItemNotFound, AccessDenied, BadRequester {
        return single(genericViews.get(id, getRequesterUserProfile()));
    }

    private List<Long> parseGraphIds(String json) throws IOException, DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<Long>> typeRef = new TypeReference<List<Long>>() {
            };
            return mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }

    private List<String> parseIds(String json) throws IOException, DeserializationError {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            return mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}
