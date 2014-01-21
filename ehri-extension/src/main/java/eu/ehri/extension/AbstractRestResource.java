package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.*;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import eu.ehri.project.models.base.Frame;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.ListMultimap;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractRestResource implements TxCheckedResource {

    public static final int DEFAULT_LIST_LIMIT = 20;
    private static final ObjectMapper mapper = new ObjectMapper();

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

    public static final String INCLUDE_PROPS_PARAM = "_ip";

    /**
     * Header names
     */
    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String PATCH_HEADER_NAME = "Patch";
    public static final String LOG_MESSAGE_HEADER_NAME = "logMessage";


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
     * Stream a single page with total, limit, and offset info.
     *
     * @param page A page of data
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingPage(
            final Query.Page<T> page) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(page, getSerializer())
                : getStreamingJsonOutput(page, getSerializer());
    }

    /**
     * Stream a single page with total, limit, and offset info, using
     * the given entity converter.
     *
     * @param page       A page of data
     * @param serializer A custom serializer instance
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingPage(
            final Query.Page<T> page, final Serializer serializer) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(page, serializer)
                : getStreamingJsonOutput(page, serializer);
    }

    private <T extends Frame> StreamingOutput getStreamingXmlOutput(final Query.Page<T> page, final Serializer serializer) {
        final Charset utf8 = Charset.forName("UTF-8");
        final String header = String.format("<page total=\"%d\" offset=\"%d\" limit=\"%d\">\n",
                page.getCount(), page.getOffset(), page.getLimit());
        final String tail = "</page>\n";

        return new StreamingOutput() {
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
        };
    }

    private <T extends Frame> StreamingOutput getStreamingJsonOutput(final Query.Page<T> page, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        final Serializer cacheSerializer = serializer.withCache();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                JsonGenerator g = f.createJsonGenerator(os);
                g.writeStartObject();
                g.writeNumberField("total", page.getCount());
                g.writeNumberField("offset", page.getOffset());
                g.writeNumberField("limit", page.getLimit());
                g.writeFieldName("values");
                g.writeStartArray();
                for (T item : page.getIterable()) {
                    try {
                        g.writeRaw('\n');
                        mapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.writeEndObject();
                g.close();
            }
        };
    }

    /**
     * Return a streaming response from an iterable.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingList(
            final Iterable<T> list) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(list, getSerializer())
                : getStreamingJsonOutput(list, getSerializer());
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param list A list of framed items
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingList(
            final Iterable<T> list, final Serializer serializer) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(list, serializer)
                : getStreamingJsonOutput(list, serializer);
    }

    private <T extends Frame> StreamingOutput getStreamingXmlOutput(final Iterable<T> list, final Serializer serializer) {
        final Charset utf8 = Charset.forName("UTF-8");
        final String header = "<list>\n";
        final String tail = "</list>\n";

        return new StreamingOutput() {
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
        };
    }

    private <T extends Frame> StreamingOutput getStreamingJsonOutput(final Iterable<T> list, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        final Serializer cacheSerializer = serializer.withCache();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();
                for (T item : list) {
                    g.writeRaw('\n');
                    try {
                        mapper.writeValue(g, cacheSerializer.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        };
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
    protected StreamingOutput streamingVertexList(
            final Iterable<Vertex> list, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        final Serializer cacheSerializer = serializer.withCache();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();
                for (Vertex item : list) {
                    try {
                        g.writeRaw('\n');
                        mapper.writeValue(g, cacheSerializer.vertexToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        };
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     * <p/>
     * FIXME: I shouldn't be here, or the other method should. Redesign API.
     *
     * @param map A map of vertices
     * @return A streaming response
     */
    protected StreamingOutput streamingVertexMap(
            final Map<String, Vertex> map, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartObject();
                for (Map.Entry<String, Vertex> keypair : map.entrySet()) {
                    try {
                        g.writeFieldName(keypair.getKey());
                        mapper.writeValue(g, serializer.vertexToData(keypair.getValue()));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndObject();
                g.close();
            }
        };
    }

    /**
     * Return a streaming response from an iterable.
     *
     * @param map A multimap of vertices
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingMultimap(
            final ListMultimap<String, T> map) {
        return streamingMultimap(map, getSerializer());
    }

    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     *
     * @param map        A map of vertices
     * @param serializer A custom serializer
     * @return A streaming response
     */
    protected <T extends Frame> StreamingOutput streamingMultimap(
            final ListMultimap<String, T> map, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartObject();
                for (String itemId : map.keySet()) {
                    g.writeFieldName(itemId);
                    g.writeStartArray();
                    for (T item : map.get(itemId)) {
                        try {
                            mapper.writeValue(g, serializer.vertexFrameToData(item));
                        } catch (SerializationError e) {
                            throw new RuntimeException(e);
                        }
                    }
                    g.writeEndArray();
                }
                g.writeEndObject();
                g.close();
            }
        };
    }

    /**
     * Return a number.
     */
    protected Response numberResponse(Long number) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? Response.ok(String.format("<count>%d</count>", number)
                .getBytes()).build()
                : Response.ok(number.toString().getBytes()).build();
    }

    /**
     * Return a boolean.
     */
    protected Response booleanResponse(boolean bool) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? Response.ok(String.format("<boolean>%s</boolean>", bool)
                .getBytes()).build()
                : Response.ok(Boolean.toString(bool).getBytes()).build();
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
}
