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
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.core.Tx;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Provides a web service interface for the PermissionGrant model.
 * 
 * FIXME: PermissionGrant is not currently an AccessibleEntity so
 * handling it is complicated. We need to re-architect the REST views
 * to handle more than just the initially-envisaged scenarios.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.PERMISSION_GRANT)
public class PermissionGrantResource extends AbstractRestResource {

    public PermissionGrantResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Fetch a given permission grant.
     *
     * @param id The ID of the permission grant
     * @return The permission grant
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getPermissionGrant(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            PermissionGrant grant = manager.getFrame(id,
                    EntityClass.PERMISSION_GRANT, PermissionGrant.class);
            Response response = single(grant);
            tx.success();
            return response;
        }
    }
    /**
     * Revoke a particular permission grant.
     *
     * @param id The ID of the permission grant
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response revokePermissionGrant(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            new AclViews(graph).revokePermissionGrant(manager.getFrame(id,
                    EntityClass.PERMISSION_GRANT, PermissionGrant.class),
                    getRequesterUserProfile());
            tx.success();
            return Response.status(Status.OK).build();
        }
    }
}
