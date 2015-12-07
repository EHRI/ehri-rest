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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.extension.base.TxCheckedResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.Query;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Base class for web service resources.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AbstractRestResource implements TxCheckedResource {

    public static final int DEFAULT_LIST_LIMIT = 20;
    public static final int ITEM_CACHE_TIME = 60 * 5; // 5 minutes

    protected static final ObjectMapper jsonMapper = new ObjectMapper();
    protected static final JsonFactory jsonFactory = new JsonFactory();

    protected static final Logger logger = LoggerFactory.getLogger(TxCheckedResource.class);
    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

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

    /**
     * Serialization config parameters.
     */
    public static final String INCLUDE_PROPS_PARAM = "_ip";

    /**
     * Header names
     */
    public static final String RANGE_HEADER_NAME = "Content-Range";
    public static final String PATCH_HEADER_NAME = "Patch";
    public static final String AUTH_HEADER_NAME = "X-User";
    public static final String LOG_MESSAGE_HEADER_NAME = "X-LogMessage";
    public static final String STREAM_HEADER_NAME = "X-Stream";


    /**
     * With each request the headers of that request are injected into the
     * requestHeaders parameter.
     */
    @Context protected HttpHeaders requestHeaders;

    @Context protected Request request;

    /**
     * Fetch the media type of the incoming request.
     *
     * @return a media type variant.
     */
    protected MediaType checkMediaType() {
        MediaType applicationJson = MediaType.APPLICATION_JSON_TYPE;
        MediaType applicationXml = MediaType.TEXT_XML_TYPE;

        // NB: Json is default so it's first...
        MediaType[] supportedTypes = new MediaType[]{applicationJson, applicationXml};
        List<Variant> variants = Variant.VariantListBuilder.newInstance()
                .mediaTypes(supportedTypes).add().build();

        Variant variant = request.selectVariant(variants);

        if (variant == null) {
            return null;
        } else {
            return variant.getMediaType();
        }
    }

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
     * @param database A Neo4j graph database
     */
    public AbstractRestResource(@Context GraphDatabaseService database) {
        graph = graphFactory.create(new TxNeo4jGraph(database));
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).build();
    }

    public FramedGraph<? extends TxGraph> getGraph() {
        return graph;
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
        Optional<List<String>> includeProps = Optional.fromNullable(uriInfo.getQueryParameters(true)
                .get(INCLUDE_PROPS_PARAM));
        return includeProps.isPresent()
                ? serializer.withIncludedProperties(includeProps.get())
                : serializer;
    }

    /**
     * Get a list of values for a given query parameter key.
     *
     * @param key the parameter name
     * @return a list of string values
     */
    protected List<String> getStringListQueryParam(String key) {
        List<String> value = uriInfo.getQueryParameters().get(key);
        return value == null ? Lists.<String>newArrayList() : value;
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
     * @param cls the class of the query object
     * @param <T> the generic type of the query object
     * @return a query object
     */
    protected <T extends AccessibleEntity> Query<T> getQuery(Class<T> cls) {
        return new Query<>(graph, cls)
                .setOffset(getIntQueryParam(OFFSET_PARAM, 0))
                .setLimit(getIntQueryParam(LIMIT_PARAM, DEFAULT_LIST_LIMIT))
                .filter(getStringListQueryParam(FILTER_PARAM))
                .orderBy(getStringListQueryParam(SORT_PARAM))
                .setStream(isStreaming());
    }

    /**
     * Retrieve the account of the current user, who may be
     * anonymous.
     *
     * @return The UserProfile
     */
    protected Accessor getRequesterUserProfile() {
        Optional<String> id = getRequesterIdentifier();
        if (!id.isPresent()) {
            return AnonymousAccessor.getInstance();
        } else {
            try {
                return manager.getFrame(id.get(), Accessor.class);
            } catch (ItemNotFound e) {
                throw new BadRequester(id.get());
            }
        }
    }

    /**
     * Retrieve the profile of the current user, throwing a
     * BadRequest if it's invalid.
     *
     * @return The current user profile
     */
    protected UserProfile getCurrentUser() {
        Accessor profile = getRequesterUserProfile();
        if (profile.isAdmin() || profile.isAnonymous()
                || !profile.getType().equals(Entities.USER_PROFILE)) {
            throw new BadRequester("Invalid user: " + profile.getId());
        }
        return graph.frame(profile.asVertex(), UserProfile.class);
    }

    /**
     * Retreive an action log message from the request header.
     *
     * @return An optional log message
     */
    protected Optional<String> getLogMessage() {
        List<String> list = requestHeaders.getRequestHeader(LOG_MESSAGE_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            try {
                return Optional.of(URLDecoder.decode(list.get(0), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                logger.error("Unsupported encoding in header: {}", e);
                return Optional.absent();
            }
        }
        return Optional.absent();
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
            return Boolean.valueOf(list.get(0));
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
            return Optional.fromNullable(list.get(0));
        }
        return Optional.absent();
    }

    /**
     * Return a default response from a single frame item.
     *
     * @param item the item
     * @param <T>  the item's generic type
     * @return a serialized representation, with location and cache control
     * headers.
     */
    protected <T extends Frame> Response single(T item) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(getRepresentation(item).getBytes(Charsets.UTF_8))
                    .location(getItemUri(item))
                    .cacheControl(getCacheControl(item)).build();
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a default response from a single vertex.
     *
     * @param item A graph vertex
     * @return A serialized representation.
     */
    protected Response single(Vertex item) {
        try {
            // FIXME: We can add cache control and location here
            return Response.status(Response.Status.OK)
                    .entity(getRepresentation(item).getBytes(Charsets.UTF_8))
                    .build();
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stream a single page with total, limit, and offset info.
     *
     * @param page A page of data
     * @param tx   the current transaction
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingPage(Query.Page<T> page, Tx tx) {
        return streamingPage(page, getSerializer(), tx);
    }

    /**
     * An abstraction class that handles streaming responses that
     * need to manage a transaction.
     * <p>
     * We cannot just return a regular StreamingResponse here
     * because then HEAD requests will leak the transaction.
     */
    static abstract class TransactionalStreamWrapper {
        protected final Tx tx;
        protected final Request request;

        public TransactionalStreamWrapper(final Request request, final Tx tx) {
            this.tx = tx;
            this.request = request;
        }

        protected Map<String, Object> getHeaders() {
            return Maps.newHashMap();
        }

        abstract StreamingOutput getStreamingOutput();

        public Response getResponse() {
            if (request.getMethod().equalsIgnoreCase("HEAD")) {
                try {
                    Response.ResponseBuilder r = Response.ok();
                    for (Map.Entry<String, Object> entry : getHeaders().entrySet()) {
                        r = r.header(entry.getKey(), entry.getValue());
                    }
                    return r.build();
                } finally {
                    tx.close();
                }
            } else {
                Response.ResponseBuilder r = Response.ok(getStreamingOutput());
                for (Map.Entry<String, Object> entry : getHeaders().entrySet()) {
                    r = r.header(entry.getKey(), entry.getValue());
                }
                return r.build();
            }
        }
    }

    static abstract class TransactionalPageStreamWrapper<T> extends TransactionalStreamWrapper {
        protected final Query.Page<T> page;

        public TransactionalPageStreamWrapper(final Request request, final Query.Page<T> page, final Tx tx) {
            super(request, tx);
            this.page = page;
        }

        protected Map<String, Object> getHeaders() {
            return ImmutableMap.<String, Object>of(
                    RANGE_HEADER_NAME,
                    String.format("offset=%d; limit=%d; total=%d",
                            page.getOffset(), page.getLimit(), page.getTotal()));
        }
    }

    /**
     * Stream a single page with total, limit, and offset info, using
     * the given entity converter.
     *
     * @param page       a page of data
     * @param serializer a custom serializer instance
     * @param tx         the current transaction
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingPage(
            final Query.Page<T> page, final Serializer serializer, final Tx tx) {
        TransactionalStreamWrapper tos = MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(page, serializer, tx)
                : getStreamingJsonOutput(page, serializer, tx);
        return tos.getResponse();
    }

    private <T extends Frame> TransactionalStreamWrapper getStreamingXmlOutput(final Query.Page<T> page, final Serializer serializer,
                                                                               final Tx tx) {
        return new TransactionalPageStreamWrapper<T>(request, page, tx) {
            @Override
            StreamingOutput getStreamingOutput() {
                final Charset utf8 = Charset.forName("UTF-8");
                final String header = String.format("<list total=\"%d\" offset=\"%d\" limit=\"%d\">%n",
                        page.getTotal(), page.getOffset(), page.getLimit());
                final String tail = String.format("</listItems>%n");
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream stream) throws IOException {
                        try {
                            stream.write(header.getBytes(utf8));
                            for (T item : page.getIterable()) {
                                stream.write(serializer.vertexFrameToXmlString(item)
                                        .getBytes(utf8));
                            }
                            stream.write(tail.getBytes(utf8));

                            tx.success();
                        } catch (SerializationError serializationError) {
                            tx.failure();
                            throw new RuntimeException(serializationError);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        };
    }

    private <T extends Frame> TransactionalStreamWrapper getStreamingJsonOutput(final Query.Page<T> page, final Serializer serializer,
                                                                                final Tx tx) {
        return new TransactionalPageStreamWrapper<T>(request, page, tx) {
            @Override
            public StreamingOutput getStreamingOutput() {
                final Serializer cacheSerializer = serializer.withCache();
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream stream) throws IOException {
                        try (JsonGenerator g = jsonFactory.createGenerator(stream)) {
                            g.writeStartArray();
                            for (T item : page.getIterable()) {
                                jsonMapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                                g.writeRaw('\n');
                            }
                            g.writeEndArray();

                            tx.success();
                        } catch (SerializationError e) {
                            tx.failure();
                            throw new RuntimeException(e);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        };
    }

    /**
     * Return a streaming response from an iterable.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingList(
            Iterable<T> list, Tx tx) {
        return streamingList(list, getSerializer(), tx);
    }

    /**
     * Return a streaming response from an iterable of item lists.
     *
     * @param lists an iterable of item groups
     * @param tx    the transaction
     * @return a streaming response
     */
    protected <T extends Frame> Response streamingListOfLists(
            Iterable<? extends Collection<T>> lists, Tx tx) {
        return getStreamingJsonGroupOutput(lists, getSerializer(), tx);
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingList(
            Iterable<T> list, Serializer serializer, Tx tx) {
        TransactionalStreamWrapper tos = MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(list, serializer, tx)
                : getStreamingJsonOutput(list, serializer, tx);
        return tos.getResponse();
    }

    private <T extends Frame> TransactionalStreamWrapper getStreamingXmlOutput(final Iterable<T> list, final Serializer serializer,
                                                                               final Tx tx) {
        return new TransactionalStreamWrapper(request, tx) {
            @Override
            StreamingOutput getStreamingOutput() {
                final Charset utf8 = Charset.forName("UTF-8");
                final String header = "<list>\n";
                final String tail = "</list>\n";
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream os) throws IOException {
                        try {
                            os.write(header.getBytes(utf8));
                            for (T item : list) {
                                os.write(serializer.vertexFrameToXmlString(item)
                                        .getBytes(utf8));
                            }
                            os.write(tail.getBytes(utf8));

                            tx.success();
                        } catch (SerializationError e) {
                            tx.failure();
                            throw new RuntimeException(e);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        };
    }

    private <T extends Frame> TransactionalStreamWrapper getStreamingJsonOutput(final Iterable<T> list, final Serializer serializer,
                                                                                final Tx tx) {
        return new TransactionalStreamWrapper(request, tx) {
            @Override
            StreamingOutput getStreamingOutput() {
                final Serializer cacheSerializer = serializer.withCache();
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream stream) throws IOException {
                        try (JsonGenerator g = jsonFactory.createGenerator(stream)) {
                            g.writeStartArray();
                            for (T item : list) {
                                g.writeRaw('\n');
                                jsonMapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                            }
                            g.writeEndArray();

                            tx.success();
                        } catch (SerializationError e) {
                            e.printStackTrace();
                            tx.failure();
                            throw new RuntimeException(e);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        };
    }

    private <T extends Frame> Response getStreamingJsonGroupOutput(
            final Iterable<? extends Collection<T>> list, final Serializer serializer, final Tx tx) {
        return new TransactionalStreamWrapper(request, tx) {
            @Override
            StreamingOutput getStreamingOutput() {
                final Serializer cacheSerializer = serializer.withCache();
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream stream) throws IOException {
                        try (JsonGenerator g = jsonFactory.createGenerator(stream)) {
                            g.writeStartArray();
                            for (Collection<T> collect : list) {
                                g.writeStartArray();
                                for (T item : collect) {
                                    jsonMapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                                }
                                g.writeEndArray();
                                g.writeRaw('\n');
                            }
                            g.writeEndArray();

                            tx.success();
                        } catch (SerializationError e) {
                            e.printStackTrace();
                            tx.failure();
                            throw new RuntimeException(e);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        }.getResponse();
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param list       an iterable of vertices
     * @param serializer a serializer object
     * @return a streaming response
     */
    protected Response streamingVertexList(
            final Iterable<Vertex> list, final Serializer serializer, final Tx tx) {
        return new TransactionalStreamWrapper(request, tx) {
            @Override
            StreamingOutput getStreamingOutput() {
                final Serializer cacheSerializer = serializer.withCache();
                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream stream) throws IOException {
                        try (JsonGenerator g = jsonFactory.createGenerator(stream)) {
                            g.writeStartArray();
                            for (Vertex item : list) {
                                jsonMapper.writeValue(g, cacheSerializer.vertexToData(item));
                                g.writeRaw('\n');
                            }
                            g.writeEndArray();

                            tx.success();
                        } catch (SerializationError e) {
                            tx.failure();
                            throw new RuntimeException(e);
                        } finally {
                            tx.close();
                        }
                    }
                };
            }
        }.getResponse();
    }

    /**
     * Get the URI for a given item.
     *
     * @param item The item
     * @return The resource URI for that item.
     */
    protected URI getItemUri(Frame item) {
        return uriInfo.getBaseUriBuilder()
                .path(item.getType())
                .path(item.getId()).build();
    }

    /**
     * Return a response from a new item with a 201 CREATED status.
     *
     * @param frame A newly-created item
     * @return A 201 response.
     * @throws SerializationError
     */
    protected Response creationResponse(Frame frame) throws SerializationError {
        return Response.status(Response.Status.CREATED).location(getItemUri(frame))
                .entity(getRepresentation(frame))
                .build();
    }

    /**
     * Get a string representation (JSON or XML) of a given frame.
     *
     * @param vertex A vertex
     * @return The string representation, according to media type
     */
    protected String getRepresentation(Vertex vertex) throws SerializationError {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getSerializer().vertexToXmlString(vertex)
                : getSerializer().vertexToJson(vertex);
    }

    /**
     * Get a string representation (JSON or XML) of a given frame.
     *
     * @param frame A framed item
     * @return The string representation, according to media type
     */
    protected String getRepresentation(Frame frame) throws SerializationError {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getSerializer().vertexFrameToXmlString(frame)
                : getSerializer().vertexFrameToJson(frame);
    }

    /**
     * Get a cache control header based on the access restrictions
     * set on the item. If it is restricted, instruct clients not
     * to cache the response.
     *
     * @param item The item
     * @return A cache control object.
     */
    protected <T extends Frame> CacheControl getCacheControl(T item) {
        CacheControl cc = new CacheControl();
        if (!(item instanceof AccessibleEntity)
                || !(((AccessibleEntity) item).hasAccessRestriction())) {
            cc.setMaxAge(ITEM_CACHE_TIME);
        } else {
            cc.setNoStore(true);
            cc.setNoCache(true);
        }
        return cc;
    }
}
