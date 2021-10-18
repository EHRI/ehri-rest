/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.ws.base;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.ws.errors.MissingOrInvalidUser;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ApiFactory;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.utils.CustomAnnotationsModule;
import eu.ehri.project.persistence.Serializer;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;


/**
 * Base class for web service resources.
 */
public abstract class AbstractResource implements TxCheckedResource {

    public static final int DEFAULT_LIST_LIMIT = QueryApi.DEFAULT_LIMIT;
    public static final int ITEM_CACHE_TIME = 60 * 5; // 5 minutes

    public static final String RESOURCE_ENDPOINT_PREFIX = "classes";

    protected static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final JsonFactory jsonFactory = jsonMapper.getFactory();

    protected static final Logger logger = LoggerFactory.getLogger(AbstractResource.class);
    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule(), new CustomAnnotationsModule());

    public static final String CSV_MEDIA_TYPE = "text/csv";

    /**
     * RDF Mimetypes and formatting mappings
     */
    public final static String TURTLE_MIMETYPE = "text/turtle";
    public final static String RDF_XML_MIMETYPE = "application/rdf+xml";
    // RDF/XML-ABBREV produces more readable XML but is very, very slow and inefficient...
    // public final static String RDF_XML_ABBREV_MIMETYPE = "application/rdf+xml+abbrev";
    public final static String N3_MIMETYPE = "application/n-triples";
    protected final BiMap<String, String> RDF_MIMETYPE_FORMATS = ImmutableBiMap.of(
            N3_MIMETYPE, "N3",
            TURTLE_MIMETYPE, "TTL",
            RDF_XML_MIMETYPE, "RDF/XML"
            // RDF_XML_ABBREV_MIMETYPE, "RDF/XML-ABBREV"
    );
    protected final String DEFAULT_RDF_FORMAT = "TTL";

    /**
     * Query arguments.
     */
    public static final String SORT_PARAM = "sort";
    public static final String FILTER_PARAM = "filter";
    public static final String LIMIT_PARAM = "limit";
    public static final String OFFSET_PARAM = "offset";
    public static final String ACCESSOR_PARAM = "accessibleTo";
    public static final String GROUP_PARAM = "group";
    public static final String ALL_PARAM = "all";
    public static final String ID_PARAM = "id";
    public static final String LOG_PARAM = "log";
    public static final String VERSION_PARAM = "version";
    public static final String SCOPE_PARAM = "scope";
    public static final String TOLERANT_PARAM = "tolerant";
    public static final String LANG_PARAM = "lang";
    public static final String COMMIT_PARAM = "commit";

    /**
     * Default to English... can't set this through config
     * unfortunately since it has to be static.
     */
    public static final String DEFAULT_LANG = "eng";

    /**
     * Serialization config parameters.
     */
    public static final String INCLUDE_PROPS_PARAM = "_ip";

    /**
     * Header names
     */
    public static final String RANGE_HEADER_NAME = "Content-Range";
    public static final String PATCH_HEADER_NAME = "X-Patch";
    public static final String AUTH_HEADER_NAME = "X-User";
    public static final String LOG_MESSAGE_HEADER_NAME = "X-LogMessage";
    public static final String STREAM_HEADER_NAME = "X-Stream";


    /**
     * With each request the headers of that request are injected into the
     * requestHeaders parameter.
     */
    @Context
    protected HttpHeaders requestHeaders;

    @Context
    protected Request request;

    /**
     * With each request URI info is injected into the uriInfo parameter.
     */
    @Context
    protected UriInfo uriInfo;

    protected final FramedGraph<? extends TxGraph> graph;
    protected final GraphManager manager;
    private final Serializer serializer;

    /**
     * Constructer.
     *
     * @param dbms A Neo4j graph database service
     */
    public AbstractResource(@Context DatabaseManagementService dbms) {
        graph = graphFactory.create(new TxNeo4jGraph(dbms, dbms.database(DEFAULT_DATABASE_NAME)));
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).build();
    }

    public FramedGraph<? extends TxGraph> getGraph() {
        return graph;
    }

    /**
     * Open a transaction on the graph.
     *
     * @return the transaction object
     */
    protected Tx beginTx() {
        return graph.getBaseGraph().beginTx();
    }

    /**
     * Get a serializer according to passed-in serialization config.
     * <p>
     * Currently the only parameter is <code>_ip=[propertyName]</code> which
     * ensures a given property is always included in the output.
     *
     * @return a vertex serializer
     */
    protected Serializer getSerializer() {
        Optional<List<String>> includeProps = Optional.ofNullable(uriInfo.getQueryParameters(true)
                .get(INCLUDE_PROPS_PARAM));
        return includeProps.map(serializer::withIncludedProperties).orElse(serializer);
    }

    /**
     * Get a list of values for a given query parameter key.
     *
     * @param key the parameter name
     * @return a list of string values
     */
    protected List<String> getStringListQueryParam(String key) {
        List<String> value = uriInfo.getQueryParameters().get(key);
        return value == null ? Lists.newArrayList() : value;
    }

    /**
     * Get an integer value for a given query parameter, falling back
     * on a default.
     *
     * @param key          the parameter name
     * @param defaultValue the default value
     * @return an integer value
     */
    protected int getIntQueryParam(String key, int defaultValue) {
        String value = uriInfo.getQueryParameters().getFirst(key);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get a query object configured according to incoming parameters.
     *
     * @return a query object
     */
    protected QueryApi getQuery() {
        return api().query()
                .withOffset(getIntQueryParam(OFFSET_PARAM, 0))
                .withLimit(getIntQueryParam(LIMIT_PARAM, DEFAULT_LIST_LIMIT))
                .filter(getStringListQueryParam(FILTER_PARAM))
                .orderBy(getStringListQueryParam(SORT_PARAM))
                .withStreaming(isStreaming());
    }

    /**
     * Throw an error if an item does not exist.
     *
     * @param id the item ID
     * @param cls the item's class
     * @throws ItemNotFound if the item is missing
     */
    protected void checkExists(String id, Class<?> cls) throws ItemNotFound {
        getRequesterUserProfile();
        manager.getEntity(id, cls);
    }

    /**
     * Check the user is either valid or anonymous.
     *
     * @throws MissingOrInvalidUser if a given user id is invalid
     */
    protected void checkUser() throws MissingOrInvalidUser {
        getRequesterUserProfile();
    }

    /**
     * Retrieve the account of the current user, who may be
     * anonymous.
     *
     * @return The UserProfile
     */
    protected Accessor getRequesterUserProfile() {
        Optional<String> id = getRequesterIdentifier();
        if (id.isEmpty()) {
            return AnonymousAccessor.getInstance();
        } else {
            try {
                return manager.getEntity(id.get(), Accessor.class);
            } catch (ItemNotFound e) {
                throw new MissingOrInvalidUser(id.get());
            }
        }
    }

    /**
     * Fetch an instance of the API with the current user.
     *
     * @return an Api instance
     */
    protected Api api() {
        return ApiFactory.withLogging(graph, getRequesterUserProfile());
    }

    /**
     * Fetch an instance of the API for anonymous access.
     *
     * @return an Api instance
     */
    protected Api anonymousApi() {
        return ApiFactory.noLogging(graph, AnonymousAccessor.getInstance());
    }

    /**
     * Retrieve the profile of the current user, throwing a
     * BadRequest if it's invalid or not a user.
     *
     * @return the current user profile
     */
    protected UserProfile getCurrentUser() {
        Accessor profile = getRequesterUserProfile();
        if (profile.isAdmin() || profile.isAnonymous()
                || !profile.getType().equals(Entities.USER_PROFILE)) {
            throw new MissingOrInvalidUser(profile.getId());
        }
        return profile.as(UserProfile.class);
    }

    /**
     * Retrieve the current actioner, which may be a user or
     * a group, throwing a bad request if it's invalid.
     *
     * @return an actioner frame
     */
    protected Actioner getCurrentActioner() {
        return getRequesterUserProfile().as(Actioner.class);
    }

    /**
     * Retrieve an action log message from the request header.
     *
     * @return An optional log message
     */
    protected Optional<String> getLogMessage() {
        List<String> list = requestHeaders.getRequestHeader(LOG_MESSAGE_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            try {
                return Optional.of(URLDecoder.decode(list.get(0), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                logger.error("Unsupported encoding in header");
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Determine if a PATCH header is present. Ideally, Jersey would
     * support the HTTP PATCH method, but it doesn't so we have to
     * user a header.
     *
     * @return Patch is given
     */
    protected Boolean isPatch() {
        List<String> list = requestHeaders.getRequestHeader(PATCH_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return Boolean.valueOf(list.get(0));
        }
        return false;
    }

    /**
     * Determine if the X-Stream header is present. This changes
     * the semantics of paged results so that no full count is
     * fetched (making it more efficient.)
     *
     * @return Patch is given
     */
    protected boolean isStreaming() {
        List<String> list = requestHeaders.getRequestHeader(STREAM_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return Boolean.parseBoolean(list.get(0));
        }
        return false;
    }

    /**
     * Retrieve the id string of the requester's user profile.
     *
     * @return the user's id, if present
     */
    private Optional<String> getRequesterIdentifier() {
        List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return Optional.ofNullable(list.get(0));
        }
        return Optional.empty();
    }

    /**
     * Return a default response from a single frame item.
     *
     * @param item the item
     * @param <T>  the item's generic type
     * @return a serialized representation, with location and cache control
     * headers.
     */
    protected <T extends Entity> Response single(T item) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(getSerializer().entityToJson(item).getBytes(Charsets.UTF_8))
                    .location(getItemUri(item))
                    .lastModified(lastModified(item))
                    .cacheControl(getCacheControl(item)).build();
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stream a single page with total, limit, and offset info.
     *
     * @param page a page of data
     * @param <T>  the item's generic type
     * @return A streaming response
     */
    protected <T extends Entity> Response streamingPage(Supplier<QueryApi.Page<T>> page) {
        return streamingPage(page, getSerializer());
    }

    /**
     * Stream a single page with total, limit, and offset info, using
     * the given entity converter.
     *
     * @param page       a page of data
     * @param serializer a custom serializer instance
     * @param <T>        the item's generic type
     * @return A streaming response
     */
    protected <T extends Entity> Response streamingPage(
            final Supplier<QueryApi.Page<T>> page, final Serializer serializer) {
        return streamingList(() -> page.get().getIterable(), serializer,
                streamingResponseBuilder(page.get()));
    }

    /**
     * Stream an iterable of vertices.
     *
     * @param vertices an iterable of vertices
     * @return a streaming response
     */
    protected Response streamingVertexList(Supplier<Iterable<Vertex>> vertices) {
        return streamingVertexList(vertices, getSerializer());
    }

    /**
     * Stream an iterable of vertices.
     *
     * @param vertices   an iterable of vertices
     * @param serializer a serializer instance
     * @return a streaming response
     */
    protected Response streamingVertexList(Supplier<Iterable<Vertex>> vertices, Serializer serializer) {
        return streamingVertexList(vertices, serializer, Response.ok());
    }

    /**
     * Return a streaming response from an iterable.
     *
     * @param list A list of framed items
     * @param <T>  the item's generic type
     * @return A streaming response
     */
    protected <T extends Entity> Response streamingList(Supplier<Iterable<T>> list) {
        return streamingList(list, getSerializer());
    }

    /**
     * Return a streaming response from an iterable of item lists.
     *
     * @param lists an iterable of item groups
     * @param <T>   the item's generic type
     * @return a streaming response
     */
    protected <T extends Entity> Response streamingListOfLists(Supplier<Iterable<? extends Collection<T>>> lists) {
        return streamingGroup(lists, getSerializer(), Response.ok());
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param list A list of framed items
     * @param serializer a serializer for writing the response
     * @param <T>  the item's generic type
     * @return A streaming response
     */
    protected <T extends Entity> Response streamingList(Supplier<Iterable<T>> list, Serializer serializer) {
        return streamingList(list, serializer, Response.ok());
    }

    /**
     * Get the URI for a given item.
     *
     * @param item The item
     * @return The resource URI for that item.
     */
    protected URI getItemUri(Entity item) {
        return uriInfo.getBaseUriBuilder()
                .path(RESOURCE_ENDPOINT_PREFIX)
                .path(item.getType())
                .path(item.getId()).build();
    }

    /**
     * Return a response from a new item with a 201 CREATED status.
     *
     * @param frame A newly-created item
     * @return a 201 response with the new item's location set
     */
    protected Response creationResponse(Entity frame) {
        try {
            return Response.status(Response.Status.CREATED)
                    .location(getItemUri(frame))
                    .entity(getSerializer().entityToJson(frame))
                    .build();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Get a cache control header based on the access restrictions
     * set on the item. If it is restricted, instruct clients not
     * to cache the response.
     *
     * @param item The item
     * @param <T>  the item's generic type
     * @return A cache control object.
     */
    protected <T extends Entity> CacheControl getCacheControl(T item) {
        CacheControl cc = new CacheControl();
        if (!(item instanceof Accessible)
                || !(((Accessible) item).hasAccessRestriction())) {
            cc.setMaxAge(ITEM_CACHE_TIME);
        } else {
            cc.setNoStore(true);
            cc.setNoCache(true);
        }
        return cc;
    }

    protected <T extends Entity> Date lastModified(T item) {
        try {
            String ts = item.as(Accessible.class).getLatestEvent().getTimestamp();
            OffsetDateTime dt = java.time.OffsetDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME);
            return Date.from(dt.toInstant());
        } catch (NullPointerException e) {
            // there's no event available for this item
            return null;
        }
    }

    /**
     * Get an RDF format for content-negotiation form
     *
     * @param format a format string, possibly null
     * @return an RDF format
     */
    protected String getRdfFormat(String format) {
        if (format == null) {
            for (String mimeValue : RDF_MIMETYPE_FORMATS.keySet()) {
                MediaType mime = MediaType.valueOf(mimeValue);
                if (requestHeaders.getAcceptableMediaTypes().contains(mime)) {
                    return RDF_MIMETYPE_FORMATS.get(mimeValue);
                }
            }
            return DEFAULT_RDF_FORMAT;
        } else {
            return RDF_MIMETYPE_FORMATS.containsValue(format) ? format : DEFAULT_RDF_FORMAT;
        }
    }

    private <T> Response.ResponseBuilder streamingResponseBuilder(QueryApi.Page<T> page) {
        Response.ResponseBuilder builder = Response.ok();
        for (Map.Entry<String, Object> entry : getHeaders(page).entrySet()) {
            builder = builder.header(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private Map<String, Object> getHeaders(QueryApi.Page<?> page) {
        return ImmutableMap.of(
                RANGE_HEADER_NAME,
                String.format("offset=%d; limit=%d; total=%d",
                        page.getOffset(), page.getLimit(), page.getTotal()));
    }

    private Response streamingVertexList(Supplier<Iterable<Vertex>> page, Serializer serializer, Response.ResponseBuilder responseBuilder) {
        return responseBuilder.entity((StreamingOutput) outputStream -> {
            final Serializer cacheSerializer = serializer.withCache();
            try (Tx tx = beginTx(); JsonGenerator g = jsonFactory.createGenerator(outputStream)) {
                g.writeStartArray();
                for (Vertex item : page.get()) {
                    g.writeRaw('\n');
                    jsonMapper.writeValue(g, item == null ? null : cacheSerializer.vertexToData(item));
                }
                g.writeEndArray();
                tx.success();
            } catch (SerializationError e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private <T extends Entity> Response streamingList(Supplier<Iterable<T>> page, Serializer serializer, Response.ResponseBuilder responseBuilder) {
        return responseBuilder.entity((StreamingOutput) outputStream -> {
            final Serializer cacheSerializer = serializer.withCache();
            try (Tx tx = beginTx(); JsonGenerator g = jsonFactory.createGenerator(outputStream)) {
                g.writeStartArray();
                for (T item : page.get()) {
                    g.writeRaw('\n');
                    jsonMapper.writeValue(g, item == null ? null : cacheSerializer.entityToData(item));
                }
                g.writeEndArray();
                tx.success();
            } catch (SerializationError e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private <T extends Entity> Response streamingGroup(Supplier<Iterable<? extends Collection<T>>> groups, Serializer serializer, Response.ResponseBuilder responseBuilder) {
        return responseBuilder.entity((StreamingOutput) outputStream -> {
            final Serializer cacheSerializer = serializer.withCache();
            try (Tx tx = beginTx(); JsonGenerator g = jsonFactory.createGenerator(outputStream)) {
                g.writeStartArray();
                for (Collection<T> collect : groups.get()) {
                    g.writeStartArray();
                    for (T item : collect) {
                        jsonMapper.writeValue(g, item == null ? null : cacheSerializer.entityToData(item));
                    }
                    g.writeEndArray();
                    g.writeRaw('\n');
                }
                g.writeEndArray();

                tx.success();
            } catch (SerializationError e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
