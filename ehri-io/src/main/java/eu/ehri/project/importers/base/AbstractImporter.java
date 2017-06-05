/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.base;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;

import java.util.List;
import java.util.Map;

/**
 * Base class for importers that import documentary units, historical agents and virtual collections,
 * with their constituent logical data, description(s), and date periods.
 *
 * @param <T> Type of node representation that can be imported. In this version,
 *            the only implementation is for {@code Map<String, Object>}.
 */
public abstract class AbstractImporter<T> {

    public static final String OBJECT_IDENTIFIER = "objectIdentifier";

    protected final PermissionScope permissionScope;
    protected final Actioner actioner;
    protected final FramedGraph<?> framedGraph;
    protected final GraphManager manager;
    protected final ImportLog log;
    private final List<ImportCallback> callbacks = Lists.newArrayList();

    /**
     * Call all registered ImportCallbacks for the given mutation.
     *
     * @param mutation the Mutation to handle callbacks for
     */
    protected void handleCallbacks(Mutation<? extends Accessible> mutation) {
        for (ImportCallback callback : callbacks) {
            callback.itemImported(mutation);
        }
    }

    public PermissionScope getPermissionScope() {
        return permissionScope;
    }

    public Actioner getActioner() {
        return actioner;
    }

    protected BundleManager getPersister(List<String> scopeIds) {
        return new BundleManager(framedGraph,
                Lists.newArrayList(Iterables.concat(permissionScope.idPath(), scopeIds)));
    }

    public BundleManager getPersister() {
        return new BundleManager(framedGraph, permissionScope.idPath());
    }

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    the permission scope
     * @param actioner the user performing the import
     * @param log      the log object
     */
    public AbstractImporter(FramedGraph<?> graph, PermissionScope scope, Actioner actioner, ImportLog log) {
        this.permissionScope = scope;
        this.framedGraph = graph;
        this.actioner = actioner;
        this.log = log;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Add a callback to run when an item is created.
     *
     * @param callback a callback function object
     */
    public void addCallback(ImportCallback callback) {
        callbacks.add(callback);
    }

    /**
     * Import an item representation into the graph, and return the Node.
     *
     * @param itemData the item representation to import
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    public abstract Accessible importItem(T itemData) throws ValidationError;

    /**
     * Import an item representation into the graph at a certain depth, and return the Node.
     *
     * @param itemData the item representation to import
     * @param scopeIds parent identifiers for ID generation,
     *                 not including permission scope
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    public abstract Accessible importItem(T itemData,
            List<String> scopeIds) throws ValidationError;

    /**
     * Extract a list of DatePeriod bundles from an item's data.
     *
     * @param data the raw map of date data
     * @return returns a List of Maps with DatePeriod.START_DATE and DatePeriod.END_DATE values
     */
    public abstract Iterable<Map<String, Object>> extractDates(T data);


    public abstract Iterable<Map<String, Object>> extractMaintenanceEvent(T itemData);

    public abstract Map<String, Object> getMaintenanceEvent(T event);
}