package eu.ehri.project.importers;

import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.BundleDAO;
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
    private final Agent repository;
    private final FramedGraph<Neo4jGraph> framedGraph;
    
    protected final ImportLog log;
    protected final T documentContext;
    protected Boolean tolerant = false;

    private List<CreationCallback> createCallbacks = new LinkedList<CreationCallback>();
    private List<CreationCallback> updateCallbacks = new LinkedList<CreationCallback>();

    public AbstractRecursiveImporter(FramedGraph<Neo4jGraph> framedGraph,
            Agent repository, ImportLog log, T documentContext) {
        this.repository = repository;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
    }

    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     * 
     * @param tolerant
     */
    public void setTolerant(Boolean tolerant) {
        this.tolerant = tolerant;
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
     * Extract child items from an item node.
     * 
     * @param itemData
     * @return
     */
    public List<T> extractChildItems(T itemData) {
        return new LinkedList<T>();
    }

    /**
     * Extract the logical DocumentaryUnit at a given depth.
     * 
     * @param itemData
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected abstract EntityBundle<DocumentaryUnit> extractDocumentaryUnit(
            T itemData, int depth) throws ValidationError;

    /**
     * Extract DocumentDescriptions at a given depth from the input data.
     * 
     * @param itemData
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected abstract List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            T itemData, int depth) throws ValidationError;

    /**
     * Extract a list of DatePeriod bundles from an item's data.
     * 
     * @param data
     * @return
     */
    public abstract List<EntityBundle<DatePeriod>> extractDates(T data);

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the
     * hierarchical depth.
     * 
     * @param itemData
     * @param parent
     * @param depth
     * @throws ValidationError
     */
    final void importItem(T itemData, DocumentaryUnit parent, int depth)
            throws ValidationError {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(itemData,
                depth);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (EntityBundle<DatePeriod> dpb : extractDates(itemData)) {
            unit.addRelation(TemporalEntity.HAS_DATE, dpb);
        }
        for (EntityBundle<DocumentDescription> dpb : extractDocumentDescriptions(
                itemData, depth)) {
            unit.addRelation(Description.DESCRIBES, dpb);
        }

        Object existingId = getExistingGraphId((String) unit.getData().get(
                AccessibleEntity.IDENTIFIER_KEY));
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
        for (T child : extractChildItems(itemData)) {
            try {
                importItem(child, frame, depth + 1);
            } catch (ValidationError e) {
                log.setErrored("Item at depth: " + depth, e.getMessage());
                if (!tolerant) {
                    throw e;
                }
            }
        }

        // Run creation callbacks for the new item...
        if (existingId == null) {
            for (CreationCallback cb : createCallbacks) {
                cb.itemImported(frame);
            }
        } else {
            for (CreationCallback cb : updateCallbacks) {
                cb.itemImported(frame);
            }
        }
    }

    /**
     * Get the entry point to the top level item data.
     * 
     * @param topLevelData
     */
    protected abstract List<T> getEntryPoints()
            throws ValidationError, InvalidInputFormatError;
    
    /**
     * Top-level entry point for importing some EAD.
     * 
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * 
     */
    public void importItems() throws ValidationError, InvalidInputFormatError {
        for (T item : getEntryPoints()) {
            importItem(item, null, 0);
        }
    }


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
                        String vid = (String) item.getProperty(AccessibleEntity.IDENTIFIER_KEY);
                        return (vid != null && vid.equals(id));
                    }
                }).id();
        return pipe.hasNext() ? pipe.next() : null;
    }
}