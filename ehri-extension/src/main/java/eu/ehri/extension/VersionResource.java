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

package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.events.Version;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful(ish) interface for accessing item versions.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 */
@Path(Entities.VERSION)
public class VersionResource extends
        AbstractAccessibleEntityResource<Version> {

    public VersionResource(@Context GraphDatabaseService database) {
        super(database, Version.class);
    }

    /**
     * Get a version item.
     *
     * @param id the version id
     * @return a version item
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws AccessDenied
     */
    @GET
    @Path("/{id:[^/]+}")
    public Response getVersion(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        return getItem(id);
    }

    /**
     * Lookup and page the versions for a given item.
     *
     * @param id the event id
     * @return a list of versions
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws AccessDenied
     */
    @GET
    @Path("/for/{id:.+}")
    public Response listFor(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            AccessibleEntity item = views
                    .setClass(AccessibleEntity.class)
                    .detail(id, user);
            return streamingPage(getQuery(Version.class).setStream(true)
                    .page(item.getAllPriorVersions(), user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }
}
