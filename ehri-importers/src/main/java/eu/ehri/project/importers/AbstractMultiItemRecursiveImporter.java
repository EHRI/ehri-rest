package eu.ehri.project.importers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Abstract base class for importers that must descend a tree like structure
 * such as EAD, where elements contain multiple child items.
 * 
 * Mea culpa: it's likely that this will only be useful for EAD.
 * 
 * This abstract class handles importing documents of type T, which contain one
 * or more logical documentary units, which may themselves contain multiple
 * child items.
 * 
 * As expressed in EAD, this looks like:
 * 
 * ead archdesc <- logical item
 * 
 * or
 * 
 * ead archdesc dsc c01 <- logical item c01 <- logical item 2 c02 <- child of
 * item 2
 * 
 * This abstract class handles the top-level logic of importing this type of
 * structure and persisting the data. Implementing concrete classes must
 * implement:
 * 
 * - Extracting from document T the entry points for each top-level and
 * returning an iterable set of item nodes T1. - Extracting from item node T1 an
 * iterable set of child nodes T2. - Extracting the item data from node T1.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public abstract class AbstractMultiItemRecursiveImporter<T> {
    private final Agent repository;
    private final FramedGraph<Neo4jGraph> framedGraph;

    protected final ImportLog log;
    protected final T documentContext;
    protected Boolean tolerant = false;

    private List<ImportCallback> createCallbacks = new LinkedList<ImportCallback>();
    private List<ImportCallback> updateCallbacks = new LinkedList<ImportCallback>();

    /**
     * Constructor.
     * 
     * @param framedGraph
     * @param repository
     * @param log
     * @param documentContext
     */
    public AbstractMultiItemRecursiveImporter(
            FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            ImportLog log, T documentContext) {
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
     * Extract child items from an item node.
     * 
     * @param itemData
     * @return
     */
    protected abstract List<T> extractChildItems(T itemData);

    /**
     * Extract the logical DocumentaryUnit at a given depth.
     * 
     * @param itemData
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected abstract Map<String, Object> extractDocumentaryUnit(T itemData,
            int depth) throws ValidationError;

    /**
     * Extract DocumentDescriptions at a given depth from the input data.
     * 
     * @param itemData
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected abstract Iterable<Map<String, Object>> extractDocumentDescriptions(
            T itemData, int depth) throws ValidationError;

    /**
     * Extract a list of DatePeriod bundles from an item's data.
     * 
     * @param data
     * @return
     */
    public abstract Iterable<Map<String, Object>> extractDates(T data);

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
        EntityBundle<DocumentaryUnit> unit = new BundleFactory<DocumentaryUnit>()
                .buildBundle(extractDocumentaryUnit(itemData, depth),
                        DocumentaryUnit.class);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            unit.addRelation(TemporalEntity.HAS_DATE,
                    new BundleFactory<DatePeriod>().buildBundle(dpb,
                            DatePeriod.class));
        }
        for (Map<String, Object> dpb : extractDocumentDescriptions(itemData,
                depth)) {
            unit.addRelation(Description.DESCRIBES,
                    new BundleFactory<DocumentDescription>().buildBundle(dpb,
                            DocumentDescription.class));
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
                // TODO: Improve error context.
                log.setErrored("Item at depth: " + depth, e.getMessage());
                if (!tolerant) {
                    throw e;
                }
            }
        }

        // Run creation callbacks for the new item...
        if (existingId == null) {
            for (ImportCallback cb : createCallbacks) {
                cb.itemImported(frame);
            }
        } else {
            for (ImportCallback cb : updateCallbacks) {
                cb.itemImported(frame);
            }
        }
    }

    /**
     * Get the entry point to the top level item data.
     * 
     * @param topLevelData
     */
    protected abstract Iterable<T> getEntryPoints() throws ValidationError,
            InvalidInputFormatError;

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
                        String vid = (String) item
                                .getProperty(AccessibleEntity.IDENTIFIER_KEY);
                        return (vid != null && vid.equals(id));
                    }
                }).id();
        return pipe.hasNext() ? pipe.next() : null;
    }
}