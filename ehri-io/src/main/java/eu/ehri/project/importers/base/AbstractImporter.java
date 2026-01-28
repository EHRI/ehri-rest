/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import eu.ehri.project.importers.ErrorCallback;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;

import java.util.List;

public abstract class AbstractImporter<I, T extends Accessible> implements ItemImporter<I, T> {

    protected final PermissionScopeFinder permissionScopeFinder;
    protected final Actioner actioner;
    protected final FramedGraph<?> framedGraph;
    protected final GraphManager manager;
    protected final ImportOptions options;
    protected final ImportLog log;
    private final List<ImportCallback> callbacks = Lists.newArrayList();
    private final List<ErrorCallback> errorCallbacks = Lists.newArrayList();

    /**
     * Call all registered ImportCallbacks for the given mutation.
     *
     * @param mutation the Mutation to handle callbacks for item events
     */
    protected void handleCallbacks(Mutation<? extends Accessible> mutation) {
        for (ImportCallback callback : callbacks) {
            callback.itemImported(mutation);
        }
    }

    public Actioner getActioner() {
        return actioner;
    }

    protected BundleManager getBundleManager(PermissionScope localScope, List<String> scopeIds) {
        return new BundleManager(framedGraph,
                Lists.newArrayList(Iterables.concat(localScope.idPath(), scopeIds)));
    }

    public BundleManager getBundleManager(String localId) {
        return new BundleManager(framedGraph, permissionScopeFinder.get(localId).idPath());
    }

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scopeFinder    the permission scope finder
     * @param actioner the user performing the import
     * @param options  the import options
     * @param log      the log object
     */
    public AbstractImporter(FramedGraph<?> graph, PermissionScopeFinder scopeFinder, Actioner actioner, ImportOptions options, ImportLog log) {
        this.permissionScopeFinder = scopeFinder;
        this.framedGraph = graph;
        this.actioner = actioner;
        this.log = log;
        this.options = options;
        manager = GraphManagerFactory.getInstance(graph);
    }

    @Override
    public void addCallback(ImportCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void addErrorCallback(ErrorCallback errorCallback) {
        errorCallbacks.add(errorCallback);
    }

    @Override
    public void handleError(Exception ex) {
        for (ErrorCallback errorCallback: errorCallbacks) {
            errorCallback.itemError(ex);
        }
    }
}