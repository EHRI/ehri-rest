/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.PermissionGrant;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a web service interface for the PermissionGrant model.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.PERMISSION_GRANT)
public class PermissionGrantResource extends AbstractResource implements DeleteResource, GetResource {

    public PermissionGrantResource(@Context DatabaseManagementService service) {
        super(service);
    }

    /**
     * Fetch a given permission grant.
     *
     * @param id The ID of the permission grant
     * @return The permission grant
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            PermissionGrant grant = manager.getEntity(id,
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
     */
    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id) throws ItemNotFound, PermissionDenied {
        try (final Tx tx = beginTx()) {
            api().acl().revokePermissionGrant(manager.getEntity(id,
                    EntityClass.PERMISSION_GRANT, PermissionGrant.class));
            tx.success();
        }
    }
}
