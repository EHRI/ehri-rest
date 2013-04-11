package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.NodeProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

import eu.ehri.project.persistance.BundleDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    protected final FramedGraph<Neo4jGraph> framedGraph;
    protected final GraphManager manager;
    protected final ImportLog log;
    protected final T documentContext;
    protected List<ImportCallback> createCallbacks = new LinkedList<ImportCallback>();
    protected List<ImportCallback> updateCallbacks = new LinkedList<ImportCallback>();
    protected BundleDAO persister;

    private NodeProperties pc;

    /**
     * Constructor.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     * @param documentContext
     */
    public AbstractImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log, T documentContext) {
        this.permissionScope = permissionScope;
        this.framedGraph = framedGraph;
        this.log = log;
        this.documentContext = documentContext;
        manager = GraphManagerFactory.getInstance(framedGraph);
        persister = new BundleDAO(framedGraph, permissionScope);
    }

    /**
     * Constructor without a document context.
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public AbstractImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
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

    abstract public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError;

    abstract public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError;

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
    protected String changeForbiddenMultivaluedProperties(String key, Object value, EntityClass entity) {
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
            String output = "";
            for (String l : (List<String>) value) {
                output += l + ", ";
            }
            logger.debug(key + " should have only 1 value. " + output.substring(0, output.length() - 2));
            return output.substring(0, output.length() - 2);
        } else {
            return value.toString();
        }
    }

}