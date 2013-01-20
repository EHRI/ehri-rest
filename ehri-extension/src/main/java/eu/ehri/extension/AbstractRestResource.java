package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.views.impl.Query;

public abstract class AbstractRestResource {

    public static final int DEFAULT_LIST_LIMIT = 20;
    
    /**
     * Query arguments.
     */
    public static final String SORT_PARAM = "sort";
    public static final String FILTER_PARAM = "filter";
    public static final String LIMIT_PARAM = "limit";
    public static final String OFFSET_PARAM = "offset";
    public static final String ACCESSOR_PARAM = "accessibleTo";
    

    /**
     * With each request the headers of that request are injected into the
     * requestHeaders parameter.
     */
    @Context
    private HttpHeaders requestHeaders;
    /**
     * With each request URI info is injected into the uriInfo parameter.
     */
    @Context
    protected UriInfo uriInfo;
    protected final GraphDatabaseService database;
    protected final FramedGraph<Neo4jGraph> graph;
    protected final GraphManager manager;
    public static final String AUTH_HEADER_NAME = "Authorization";
    protected final Converter converter;

    public AbstractRestResource(@Context GraphDatabaseService database) {
        this.database = database;
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
        manager = GraphManagerFactory.getInstance(graph);
        converter  = new Converter(graph);
    }

    /**
     * Retrieve the id of the UserProfile of the requester
     * 
     * @return The UserProfile
     * @throws BadRequester
     */
    protected Accessor getRequesterUserProfile() throws BadRequester {
        String id = getRequesterIdentifier();
        if (id == null) {
            return AnonymousAccessor.getInstance();
        } else {
            try {
                return manager.getFrame(id, Accessor.class);
            } catch (ItemNotFound e) {
                throw new BadRequester(id);
            }
        }
    }

    /**
     * Retreive the id string of the requester's UserProfile.
     * 
     * @return
     */
    private String getRequesterIdentifier() {
        List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Fetch an entity of a given type by its identifier.
     * 
     * @param typeName
     * @param name
     * @param cls
     * @return
     * @throws ItemNotFound
     */
    protected <E> E getEntity(String typeName, String name, Class<E> cls)
            throws ItemNotFound {
        return graph.frame(manager.getVertex(name), cls);
    }
    
    /**
     * Stream a single page with total, limit, and offset info.
     * 
     * @param page
     * @return
     */
    protected <T extends VertexFrame> StreamingOutput streamingPage(
            final Query.Page<T> page) {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(os);
                g.writeStartObject();
                g.writeNumberField("total", page.getCount());
                g.writeNumberField("offset", page.getOffset());
                g.writeNumberField("limit", page.getLimit());
                g.writeFieldName("values");
                g.writeStartArray();
                for (T item : page.getIterable()) {
                    try {
                        mapper.writeValue(g, converter.vertexFrameToData(item));
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
    protected <T extends VertexFrame> StreamingOutput streamingList(
            final Iterable<T> list) {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();
        final Converter converter = new Converter(graph);
        // FIXME: I don't understand this streaming output system well
        // enough
        // to determine whether this actually streams or not. It certainly
        // doesn't look like it.
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();
                for (T item : list) {
                    try {
                        mapper.writeValue(g, converter.vertexFrameToData(item));
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
     * Return a streaming response from an iterable.
     * 
     * @param list
     * @return
     */
    protected <T extends VertexFrame> StreamingOutput streamingMultimap(
            final ListMultimap<String, T> map) {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();
        final Converter converter = new Converter(graph);
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartObject();
                for (String itemId : map.keySet()) {
                    g.writeFieldName(itemId);
                    g.writeStartArray();
                    for (T item : map.get(itemId)) {
                        try {
                            mapper.writeValue(g, converter.vertexFrameToData(item));
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
}