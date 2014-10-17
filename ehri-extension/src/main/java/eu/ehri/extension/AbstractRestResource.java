package eu.ehri.extension;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.extension.base.TxCheckedResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import eu.ehri.project.views.Query;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;


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

    public static final String INCLUDE_PROPS_PARAM = "_ip";

    /**
     * Header names
     */
    public static final String RANGE_HEADER_NAME = "Content-Range";
    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String PATCH_HEADER_NAME = "Patch";
    public static final String LOG_MESSAGE_HEADER_NAME = "logMessage";
    public static final String STREAM_HEADER_NAME = "X-Stream";


    /**
     * With each request the headers of that request are injected into the
     * requestHeaders parameter.
     */
    @Context
    private HttpHeaders requestHeaders;

    @Context
    private Request request;


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

    protected final GraphDatabaseService database;
    protected final FramedGraph<TxCheckedNeo4jGraph> graph;
    protected final GraphManager manager;
    private final Serializer serializer;

    public AbstractRestResource(@Context GraphDatabaseService database) {
        this.database = database;
        graph = graphFactory.create(new TxCheckedNeo4jGraph(database));
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).build();
    }

    public Serializer getSerializer() {
        Optional<List<String>> includeProps = Optional.fromNullable(uriInfo.getQueryParameters(true)
                .get(INCLUDE_PROPS_PARAM));
        return includeProps.isPresent()
                ? serializer.withIncludedProperties(includeProps.get())
                : serializer;
    }

    public FramedGraph<TxCheckedNeo4jGraph> getGraph() {
        return graph;
    }

    protected List<String> getStringListQueryParam(String key) {
        List<String> value = uriInfo.getQueryParameters().get(key);
        return value == null ? Lists.<String>newArrayList() : value;
    }

    protected int getIntQueryParam(String key, int defaultValue) {
        String value = uriInfo.getQueryParameters().getFirst(key);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected <T extends AccessibleEntity> Query<T> getQuery(Class<T> cls) {
        return new Query<T>(graph, cls)
                .setOffset(getIntQueryParam(OFFSET_PARAM, 0))
                .setLimit(getIntQueryParam(LIMIT_PARAM, DEFAULT_LIST_LIMIT))
                .filter(getStringListQueryParam(FILTER_PARAM))
                .orderBy(getStringListQueryParam(SORT_PARAM))
                .setStream(isStreaming());
    }

    /**
     * If graph is in a transaction, roll it back. Otherwise,
     * do nothing.
     */
    protected void cleanupTransaction() {
        if (graph.getBaseGraph().isInTransaction()) {
            logger.error("Rolling back active transaction");
            graph.getBaseGraph().rollback();
        }
    }


    /**
     * Retrieve the account of the current user, who may be
     * anonymous.
     *
     * @return The UserProfile
     * @throws BadRequester
     */
    protected Accessor getRequesterUserProfile() throws BadRequester {
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
    protected UserProfile getCurrentUser() throws BadRequester {
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
            return Optional.of(list.get(0));
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
     * Retreive the id string of the requester's UserProfile.
     *
     * @return String ID
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
     * @param item The item
     * @param <T>  The item's generic type
     * @return A serialized representation, with location and cache control
     *         headers.
     */
    protected <T extends Frame> Response single(final T item) {
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
    protected Response single(final Vertex item) {
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
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingPage(
            final Query.Page<T> page) {
        return streamingPage(page, getSerializer());
    }

    /**
     * Stream a single page with total, limit, and offset info, using
     * the given entity converter.
     *
     * @param page       A page of data
     * @param serializer A custom serializer instance
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingPage(
            final Query.Page<T> page, final Serializer serializer) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(page, serializer)
                : getStreamingJsonOutput(page, serializer);
    }

    /**
     * Return XML output from a page of data.
     *
     * @param page       The page object
     * @param serializer The serializer
     * @param <T>        The type of item in the page
     * @return An XML response.
     */
    private <T extends Frame> Response getStreamingXmlOutput(final Query.Page<T> page, final Serializer serializer) {
        final Charset utf8 = Charset.forName("UTF-8");
        final String header = String.format("<list total=\"%d\" offset=\"%d\" limit=\"%d\">%n",
                page.getTotal(), page.getOffset(), page.getLimit());
        final String tail = String.format("</listItems>%n");

        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                os.write(header.getBytes(utf8));
                try {
                    for (T item : page.getIterable()) {
                        os.write(serializer.vertexFrameToXmlString(item)
                                .getBytes(utf8));
                    }
                } catch (SerializationError serializationError) {
                    throw new RuntimeException(serializationError);
                }
                os.write(tail.getBytes(utf8));
            }
        }).header(RANGE_HEADER_NAME, getPaginationResponseHeader(page))
                .build();
    }

    /**
     * Get a response header string for a page of data.
     * <p/>
     * NB: Subject to change!
     *
     * @param page The input page
     * @return The pagination data formatted as a string.
     */
    private String getPaginationResponseHeader(Query.Page<?> page) {
        return String.format("offset=%d; limit=%d; total=%d",
                page.getOffset(), page.getLimit(), page.getTotal());
    }

    /**
     * Return a JSON output from a page of data
     *
     * @param page       The page object
     * @param serializer The serializer
     * @param <T>        The type of item in the page
     * @return A JSON response
     */
    private <T extends Frame> Response getStreamingJsonOutput(final Query.Page<T> page, final Serializer serializer) {
        final Serializer cacheSerializer = serializer.withCache();
        StreamingOutput output = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                JsonGenerator g = jsonFactory.createJsonGenerator(os);
                g.writeStartArray();
                for (T item : page.getIterable()) {
                    try {
                        g.writeRaw('\n');
                        jsonMapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        };
        return Response.ok(output)
                .header(RANGE_HEADER_NAME, getPaginationResponseHeader(page))
                .build();
    }

    /**
     * Return a streaming response from an iterable.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingList(
            final Iterable<T> list) {
        return streamingList(list, getSerializer());
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> Response streamingList(
            final Iterable<T> list, final Serializer serializer) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(list, serializer)
                : getStreamingJsonOutput(list, serializer);
    }

    private <T extends Frame> Response getStreamingXmlOutput(final Iterable<T> list, final Serializer serializer) {
        final Charset utf8 = Charset.forName("UTF-8");
        final String header = "<list>\n";
        final String tail = "</list>\n";

        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                os.write(header.getBytes(utf8));
                try {
                    for (T item : list) {
                        os.write(serializer.vertexFrameToXmlString(item)
                                .getBytes(utf8));
                    }
                } catch (SerializationError e) {
                    throw new RuntimeException(e);
                }
                os.write(tail.getBytes(utf8));
            }
        }).build();
    }

    private <T extends Frame> Response getStreamingJsonOutput(final Iterable<T> list, final Serializer serializer) {
        final Serializer cacheSerializer = serializer.withCache();
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = jsonFactory.createJsonGenerator(arg0);
                g.writeStartArray();
                for (T item : list) {
                    g.writeRaw('\n');
                    try {
                        jsonMapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        }).build();
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     * <p/>
     * FIXME: I shouldn't be here, or the other method should. Redesign API.
     *
     * @param list A list of vertices
     * @return A streaming response
     */
    protected Response streamingVertexList(
            final Iterable<Vertex> list, final Serializer serializer) {
        final Serializer cacheSerializer = serializer.withCache();
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = jsonFactory.createJsonGenerator(arg0);
                g.writeStartArray();
                for (Vertex item : list) {
                    try {
                        g.writeRaw('\n');
                        jsonMapper.writeValue(g, cacheSerializer.vertexToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        }).build();
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
