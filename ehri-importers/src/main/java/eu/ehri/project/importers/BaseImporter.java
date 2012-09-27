package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.util.iterators.SingleIterator;

import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public abstract class BaseImporter<T> implements Importer<T> {
    private static final String IDENTITY_KEY = "identifier";
    private Agent repository;
    private FramedGraph<Neo4jGraph> framedGraph;

    private List<CreationCallback> callBacks = new LinkedList<CreationCallback>();

    public BaseImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository) {
        this.repository = repository;
        this.framedGraph = framedGraph;
    }

    /**
     * Add a callback to run when an item is created.
     * 
     * @param cb
     */
    public void addCreationCallback(final CreationCallback cb) {
        callBacks.add(cb);
    }

    public List<T> extractChildData(T data) {
        return new LinkedList<T>();
    }

    protected EntityBundle<DocumentaryUnit> extractDocumentaryUnit(T data,
            int depth) throws Exception {

        EntityBundle<DocumentaryUnit> bundle = new BundleFactory<DocumentaryUnit>()
                .buildBundle(new HashMap<String, Object>(),
                        DocumentaryUnit.class);

        // Extract details for the logical item here

        return bundle;
    }

    abstract List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            T data, int depth) throws Exception;

    public List<EntityBundle<DatePeriod>> extractDates(T data) throws Exception {
        return new LinkedList<EntityBundle<DatePeriod>>();
    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the
     * hierarchical depth.
     * 
     * @param data
     * @param parent
     * @param depth
     * @throws Exception
     */
    protected void importItem(T data, DocumentaryUnit parent, int depth)
            throws Exception {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(data, depth);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);

        // Add dates to the bundle since they're @Dependent relations.
        for (EntityBundle<DatePeriod> dpb : extractDates(data)) {
            unit = unit.addRelation(TemporalEntity.HAS_DATE, dpb);
        }

        Object id = getExistingGraphId((String) unit.getData()
                .get(IDENTITY_KEY));
        DocumentaryUnit frame;
        if (id != null) {
            frame = persister.update(new EntityBundle<DocumentaryUnit>(id, unit
                    .getData(), unit.getBundleClass(), unit.getRelations()));
        } else {
            frame = persister.create(unit);

            // Set the repository/item relationship
            repository.addCollection(frame);
        }

        // Set the parent child relationship
        if (parent != null)
            parent.addChild(frame);

        // Save Descriptions
        {
            BundleDAO<DocumentDescription> descPersister = new BundleDAO<DocumentDescription>(
                    framedGraph);
            for (EntityBundle<DocumentDescription> dpb : extractDocumentDescriptions(
                    data, depth)) {
                frame.addDescription(descPersister.create(dpb));
            }
        }

        // Search through child parts and add them recursively...
        for (T child : extractChildData(data)) {
            importItems(child, frame, depth + 1);
        }

        // Run creation callbacks for the new item...
        for (CreationCallback cb : callBacks) {
            cb.itemImported(frame);
        }
    }

    /**
     * Entry point for a top-level DocumentaryUnit item.
     */
    protected void importItem(T data) throws Exception {
        importItem(data, null, 0);
    }

    protected void importItems(T data, DocumentaryUnit parent, int depth)
            throws Exception {
        importItem(data, parent, depth);
    }

    /**
     * Import (multiple) items from the top-level data.
     * 
     * @param data
     * @param agent
     * @throws Exception
     */
    public void importItems(T data) throws Exception {
        importItems(data, null, 0);
    }

    /**
     * Main entry-point to trigger parsing.
     * 
     */
    public abstract void importItems() throws Exception;

    // Helpers.

    /**
     * Lookup the graph ID of an existing object based on the IDENTITY_KEY
     * 
     * @param id
     * @return
     */
    private Object getExistingGraphId(final String id) {
        // Lookup the graph id of an object with the same
        // identity key...
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GremlinPipeline pipe = new GremlinPipeline(repository.asVertex())
                .out(Agent.HOLDS).filter(new PipeFunction<Vertex, Boolean>() {
                    public Boolean compute(Vertex item) {
                        String vid = (String) item.getProperty(IDENTITY_KEY);
                        if (vid != null)
                            return vid == id;
                        return false;
                    }
                }).id();
        return pipe.hasNext() ? pipe.next() : null;
    }
}