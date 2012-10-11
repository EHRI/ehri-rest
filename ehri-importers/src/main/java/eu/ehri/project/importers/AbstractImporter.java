package eu.ehri.project.importers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ValidationError;
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
 * Base class for importers that import documentary units, with their
 * constituent logical data, description(s), and date periods.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public abstract class AbstractImporter<T> {

    protected final Agent repository;
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final ImportLog log;
    protected final T documentContext;
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
    public AbstractImporter(FramedGraph<Neo4jGraph> framedGraph,
            Agent repository, ImportLog log, T documentContext) {
        this.repository = repository;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
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
     * @throws IntegrityError 
     */
    protected DocumentaryUnit importItem(T itemData, DocumentaryUnit parent,
            int depth) throws ValidationError, IntegrityError {
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
        return frame;
    }

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