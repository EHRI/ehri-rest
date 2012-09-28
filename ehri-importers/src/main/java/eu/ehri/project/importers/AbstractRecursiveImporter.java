package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidInputDataError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Abstract base class for importers that must descend a tree like structure
 * such as EAD, where elements contain multiple child items.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public abstract class AbstractRecursiveImporter<T> implements Importer<T> {
    private static final String IDENTITY_KEY = "identifier";
    private Agent repository;
    private FramedGraph<Neo4jGraph> framedGraph;

    private List<CreationCallback> createCallbacks = new LinkedList<CreationCallback>();
    private List<CreationCallback> updateCallbacks = new LinkedList<CreationCallback>();

    public AbstractRecursiveImporter(FramedGraph<Neo4jGraph> framedGraph,
            Agent repository) {
        this.repository = repository;
        this.framedGraph = framedGraph;
    }

    /**
     * Add a callback to run when an item is created.
     * 
     * @param cb
     */
    public void addCreationCallback(final CreationCallback cb) {
        createCallbacks.add(cb);
    }
    
    public void addUpdateCallback(final CreationCallback cb) {
    	updateCallbacks.add(cb);
    }

    /**
     * Extract child items.
     * 
     * @param data
     * @return
     */
    public List<T> extractChildData(T data) {
        return new LinkedList<T>();
    }

    /**
     * Extract the logical DocumentaryUnit at a given depth.
     * 
     * @param data
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected EntityBundle<DocumentaryUnit> extractDocumentaryUnit(T data,
            int depth) throws ValidationError {

        EntityBundle<DocumentaryUnit> bundle = new BundleFactory<DocumentaryUnit>()
                .buildBundle(new HashMap<String, Object>(),
                        DocumentaryUnit.class);

        // Extract details for the logical item here

        return bundle;
    }

    /**
     * Extract DocumentDescriptions at a given depth from the input data.
     * 
     * @param data
     * @param depth
     * @return
     * @throws ValidationError
     */
    public abstract List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            T data, int depth) throws ValidationError;

    public abstract List<EntityBundle<DatePeriod>> extractDates(T data);

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the
     * hierarchical depth.
     * 
     * @param data
     * @param parent
     * @param depth
     * @throws ValidationError
     */
    protected void importSingleItemAtDepth(T data, DocumentaryUnit parent,
            int depth) throws ValidationError {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(data, depth);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (EntityBundle<DatePeriod> dpb : extractDates(data)) {
            unit.addRelation(TemporalEntity.HAS_DATE, dpb);
        }
        for (EntityBundle<DocumentDescription> dpb : extractDocumentDescriptions(
                data, depth)) {
            unit.addRelation(Description.DESCRIBES, dpb);
        }

        Object existingId = getExistingGraphId((String) unit.getData().get(
                IDENTITY_KEY));
        DocumentaryUnit frame;
        if (existingId != null) {
            frame = persister.update(new EntityBundle<DocumentaryUnit>(
                    existingId, unit.getData(), unit.getBundleClass(), unit
                            .getRelations()));
        } else {
            frame = persister.create(unit);

            // Set the repository/item relationship
            repository.addCollection(frame);
        }

        // Set the parent child relationship
        if (parent != null)
            parent.addChild(frame);

        // Search through child parts and add them recursively...
        for (T child : extractChildData(data)) {
            importMultipleItemsAtDepth(child, frame, depth + 1);
        }

        // Run creation callbacks for the new item...
        for (CreationCallback cb : createCallbacks) {
            cb.itemImported(frame);
        }
    }

    /**
     * Entry point for a top-level DocumentaryUnit item.
     */
    protected void importItem(T data) throws ValidationError {
        importSingleItemAtDepth(data, null, 0);
    }

    /**
     * Import multiple items with a given parent at a given depth.
     * 
     * @param data
     * @param parent
     * @param depth
     * @throws ValidationError
     */
    protected abstract void importMultipleItemsAtDepth(T data,
            DocumentaryUnit parent, int depth) throws ValidationError;

    /**
     * Import (multiple) items from the top-level data.
     * 
     * @param data
     * @param agent
     * @throws ValidationError
     */
    public void importItemsFromData(T data) throws ValidationError {
        importMultipleItemsAtDepth(data, null, 0);
    }

    /**
     * Main entry-point to trigger parsing.
     * 
     * @throws InvalidInputDataError
     * 
     */
    public abstract void importItems() throws ValidationError,
            InvalidInputDataError;

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