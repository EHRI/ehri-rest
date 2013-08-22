package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
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
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.views.Query;

public abstract class AbstractRestResource implements TxCheckedResource {

    public static final int DEFAULT_LIST_LIMIT = 20;
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Query arguments.
     */
    public static final String SORT_PARAM = "sort";
    public static final String FILTER_PARAM = "filter";
    public static final String LIMIT_PARAM = "limit";
    public static final String OFFSET_PARAM = "offset";
    public static final String ACCESSOR_PARAM = "accessibleTo";
    public static final String GROUP_PARAM = "group";

    /**
     * Header names
     */
    public static final String AUTH_HEADER_NAME = "Authorization";
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
    protected final Serializer serializer;

    public AbstractRestResource(@Context GraphDatabaseService database) {
        this.database = database;
        graph = new FramedGraphFactory(
                new JavaHandlerModule()).create(new TxCheckedNeo4jGraph(database));
        manager = GraphManagerFactory.getInstance(graph);
        serializer  = new Serializer.Builder(graph).withLiteMode(true).build();
    }

    public FramedGraph<TxCheckedNeo4jGraph> getGraph() {
        return graph;
    }

    /**
     * Retrieve the id of the UserProfile of the requester
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
     * Retreive an action log message from the request header.
     *
     * @return
     */
    protected Optional<String> getLogMessage() {
        List<String> list = requestHeaders.getRequestHeader(LOG_MESSAGE_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        return Optional.absent();
    }

    /**
     * Retreive the id string of the requester's UserProfile.
     * 
     * @return
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
     * @param page
     * @return
     */
    protected <T extends Frame> StreamingOutput streamingPage(
            final Query.Page<T> page) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(page, serializer)
                : getStreamingJsonOutput(page, serializer);
    }
    
    /**
     * Stream a single page with total, limit, and offset info, using
     * the given entity converter.
     * 
     * @param page
     * @return
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
     * @param list
     * @return
     */
    protected <T extends Frame> StreamingOutput streamingList(
            final Iterable<T> list) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? getStreamingXmlOutput(list, serializer)
                : getStreamingJsonOutput(list, serializer);
    }
        
    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     * 
     * @param list
     * @return
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
     *
     * FIXME: I shouldn't be here, or the other method should. Redesign API.
     *
     * @param list
     * @return
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
     *
     * FIXME: I shouldn't be here, or the other method should. Redesign API.
     *
     * @param map
     * @return
     */
    protected StreamingOutput streamingVertexMap(
            final Map<String, Vertex> map, final Serializer serializer) {
        final JsonFactory f = new JsonFactory();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartObject();
                for (Map.Entry<String,Vertex> keypair: map.entrySet()) {
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
     * Return a number.
     */
    protected Response numberResponse(Long number) {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? Response.ok(String.format("<count>%d</count>", number.longValue())
                    .getBytes()).build()
                : Response.ok(number.toString().getBytes()).build();
    }

    /**
     * Return a streaming response from an iterable.
     * 
     * @param map
     * @return
     */
    protected <T extends Frame> StreamingOutput streamingMultimap(
            final ListMultimap<String, T> map) {
        return streamingMultimap(map, serializer);
    }
        
    /**
     * Return a streaming response from an iterable, using the given
     * entity converter.
     * 
     * @param map
     * @param serializer
     * @return
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
     * Get a string representation (JSON or XML) of a given frame.
     * @param vertex
     * @return
     */
    protected String getRepresentation(Vertex vertex) throws SerializationError {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? serializer.vertexToXmlString(vertex)
                : serializer.vertexToJson(vertex);
    }

    /**
     * Get a string representation (JSON or XML) of a given frame.
     * @param frame
     * @return
     */
    protected String getRepresentation(Frame frame) throws SerializationError {
        return MediaType.TEXT_XML_TYPE.equals(checkMediaType())
                ? serializer.vertexFrameToXmlString(frame)
                : serializer.vertexFrameToJson(frame);
    }
}