package eu.ehri.project.importers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * 
 * @author paulboon
 *
 */
public abstract class AbstractCVocImporter<T> {

    protected final Agent repository;
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final GraphManager manager;
    protected final ImportLog log;
    protected final T documentContext;
    protected List<ImportCallback> createCallbacks = new LinkedList<ImportCallback>();
    protected List<ImportCallback> updateCallbacks = new LinkedList<ImportCallback>();

    /**
     * Constructor.
     * 
     * @param framedGraph
     * @param repository
     * @param log
     * @param documentContext
     */
    public AbstractCVocImporter(FramedGraph<Neo4jGraph> framedGraph,
    		Agent repository, ImportLog log, T documentContext) {
        this.repository = repository;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
        manager = GraphManagerFactory.getInstance(framedGraph);
    }

      public AbstractCVocImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository, ImportLog log) {
        this.repository = repository;
        this.framedGraph = framedGraph;
        this.log = log;
        documentContext=null;
        manager = GraphManagerFactory.getInstance(framedGraph);
    }
      
    /**
     * Add a callback to run when an item is created.
     * 
     * @param cb
     */
    public void addCreationCallback(final ImportCallback cb) {
        createCallbacks.add(cb);
    }

    /**
     * Add a callback to run when an item is updated.
     * 
     * @param cb
     */
    public void addUpdateCallback(final ImportCallback cb) {
        updateCallbacks.add(cb);
    }

   /**
    * The actual importing
    * 
    * @param itemData
    * @param depth
    * @return
    * @throws ValidationError
    */
    abstract public AccessibleEntity importItem(Map<String, Object> itemData, 
            int depth) throws ValidationError;
    
    /**
     * Extract a list of DatePeriod bundles from an item's data.
     * 
     * @param data
     * @return
     */
    public abstract Iterable<Map<String, Object>> extractDates(T data);


    /**
     * Lookup the graph ID of an existing object based on the IDENTITY_KEY
     * 
     * @param id
     * @return
     */
    /*
    protected Object getExistingGraphId(final String id) {
        // Lookup the graph id of an object with the same
        // identity key...
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GremlinPipeline pipe = new GremlinPipeline(repository.asVertex())
                .out(Repository.HOLDS).filter(new PipeFunction<Vertex, Boolean>() {
                    public Boolean compute(Vertex item) {
                        String vid = (String) item
                                .getProperty(AccessibleEntity.IDENTIFIER_KEY);
                        return (vid != null && vid.equals(id));
                    }
                }).id();
        return pipe.hasNext() ? pipe.next() : null;
    }
    */
}