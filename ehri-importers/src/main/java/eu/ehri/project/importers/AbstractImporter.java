package eu.ehri.project.importers;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.NodeProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

import eu.ehri.project.persistence.BundleDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for importers that import documentary units, with their
 * constituent logical data, description(s), and date periods.
 *
 * @param <T>
 * @author michaelb
 */
public abstract class AbstractImporter<T> {

    private static final String NODE_PROPERTIES = "allowedNodeProperties.csv";

    private static final Logger logger = LoggerFactory.getLogger(AbstractImporter.class);
    protected final PermissionScope permissionScope;
    protected final FramedGraph<?> framedGraph;
    protected final GraphManager manager;
    protected final ImportLog log;
    protected final T documentContext;
    protected List<ImportCallback> createCallbacks = new LinkedList<ImportCallback>();
    protected List<ImportCallback> updateCallbacks = new LinkedList<ImportCallback>();
    protected List<ImportCallback> unchangedCallbacks = new LinkedList<ImportCallback>();

    private NodeProperties pc;
    private Joiner stringJoiner = Joiner.on("\n\n").skipNulls();

    protected void handleCallbacks(Mutation<? extends AccessibleEntity> mutation) {
        switch (mutation.getState()) {
            case CREATED:
                for (ImportCallback cb: createCallbacks)
                    cb.itemImported(mutation.getNode());
                break;
            case UPDATED:
                for (ImportCallback cb: updateCallbacks)
                    cb.itemImported(mutation.getNode());
                break;
            case UNCHANGED:
                for (ImportCallback cb: unchangedCallbacks)
                    cb.itemImported(mutation.getNode());
                break;
        }
    }

    public PermissionScope getPermissionScope() {
        return permissionScope;
    }

    public BundleDAO getPersister(List<String> scopeIds) {
        return new BundleDAO(framedGraph,
                Iterables.concat(permissionScope.idPath(), scopeIds));
    }

    public BundleDAO getPersister() {
        return new BundleDAO(framedGraph, permissionScope.idPath());
    }

    /**
     * Constructor.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     * @param documentContext
     */
    public AbstractImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log,
            T documentContext) {
        this.permissionScope = permissionScope;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
        manager = GraphManagerFactory.getInstance(framedGraph);
    }

    /**
     * Constructor without a document context.
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public AbstractImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        this(framedGraph, permissionScope, log, null);
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
     * Add a callback to run when an item is left unchanged.
     *
     * @param cb
     */
    public void addUnchangedCallback(final ImportCallback cb) {
        unchangedCallbacks.add(cb);
    }

    /**
     * Import an item representation into the graph, and return the Node.
     * 
     * @param itemData the item representation to import
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    abstract public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError;

    /**
     * Import an item representation into the graph at a certain depth, and return the Node.
     * 
     * @param itemData the item representation to import
     * @param scopeIds parent identifiers for ID generation,
     *                 not including permission scope
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    abstract public AccessibleEntity importItem(Map<String, Object> itemData,
            List<String> scopeIds) throws ValidationError;

    /**
     * Extract a list of DatePeriod bundles from an item's data.
     *
     * @param data
     * @return returns a List of Maps with DatePeriod.START_DATE and DatePeriod.END_DATE values
     */
    public abstract Iterable<Map<String, Object>> extractDates(T data);


    /**
     * only properties that have the multivalued-status can actually be multivalued. all other properties will be
     * flattened by this method.
     *
     * @param key
     * @param value
     * @param entity - the EntityClass with which this frameMap must comply
     */
    protected Object changeForbiddenMultivaluedProperties(String key, Object value, EntityClass entity) {
        if (pc == null) {
            pc = new NodeProperties();
            try {
                InputStream fis = getClass().getClassLoader().getResourceAsStream(NODE_PROPERTIES);
                if (fis == null) {
                    throw new RuntimeException("Missing properties file: " + NODE_PROPERTIES);
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
                String firstline = br.readLine();
                pc.setTitles(firstline);

                String line;
                while ((line = br.readLine()) != null) {
                    pc.addRow(line);
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        if (value instanceof List
                && (!pc.hasProperty(entity.getName(), key) || !pc.isMultivaluedProperty(entity.getName(), key))) {
            return stringJoiner.join((List<String>) value);
        } else {
            return value;
        }
    }
}