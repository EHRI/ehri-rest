package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public abstract class BaseImporter<T> implements Importer<T> {
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

    protected void importItem(T data, DocumentaryUnit parent, int depth)
            throws Exception {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(data, depth);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);
        DocumentaryUnit frame = persister.create(unit);

        // Set the parent child relationship
        if (parent != null)
            parent.addChild(frame);

        // Set the repository/item relationship
        repository.addCollection(frame);

        // Save DatePeriods... this is not an idempotent step, and will need
        // to be radically altered when updating existing items...
        {
            BundleDAO<DatePeriod> datePersister = new BundleDAO<DatePeriod>(
                    framedGraph);
            for (EntityBundle<DatePeriod> dpb : extractDates(data)) {
                frame.addDatePeriod(datePersister.create(dpb));
            }
        }

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
            cb.itemCreated(frame);
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
}