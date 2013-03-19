package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for importers that import documentary units, with their
 * constituent logical data, description(s), and date periods.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public abstract class AbstractImporter<T> {

    protected final PermissionScope permissionScope;
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
     * @param permissionScope
     * @param log
     * @param documentContext
     */
    public AbstractImporter(FramedGraph<Neo4jGraph> framedGraph,
            PermissionScope permissionScope, ImportLog log, T documentContext) {
        this.permissionScope = permissionScope;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
         manager = GraphManagerFactory.getInstance(framedGraph);
    }

      public AbstractImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        this.permissionScope = permissionScope;
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

   
    abstract public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError;
    /**
     * Extract a list of DatePeriod bundles from an item's data.
     * 
     * @param data
     * @return returns a List of Maps with DatePeriod.START_DATE and DatePeriod.END_DATE values
     */
    public abstract Iterable<Map<String, Object>> extractDates(T data);
}