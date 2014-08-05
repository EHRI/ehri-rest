package eu.ehri.extension.providers;

import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Provider
public class FrameProvider implements MessageBodyWriter<Frame> {

    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());


    private final FramedGraph<TxCheckedNeo4jGraph> graph;
    private final GraphManager manager;
    private final Serializer serializer;

    public FrameProvider(@Context GraphDatabaseService databaseService) {
        super();
        graph = graphFactory.create(new TxCheckedNeo4jGraph(databaseService));
        manager = GraphManagerFactory.getInstance(graph);
        serializer = new Serializer.Builder(graph).build();
    }

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return Frame.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(Frame frame, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(Frame frame, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> stringObjectMultivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
        try {
            outputStream.write(serializer.vertexFrameToJson(frame).getBytes());
        } catch (SerializationError serializationError) {
            throw new IOException(serializationError);
        }
    }
}
