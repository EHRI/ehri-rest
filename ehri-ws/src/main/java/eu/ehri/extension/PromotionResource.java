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

import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.views.PromotionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Endpoints for promoting and demoting items.
 */
@Path(PromotionResource.ENDPOINT)
public class PromotionResource extends AbstractRestResource {

    public static final String ENDPOINT = "promote";
    private final PromotionViews pv;

    public PromotionResource(@Context GraphDatabaseService database) {
        super(database);
        pv = new PromotionViews(graph);
    }

    /**
     * Up vote an item.
     *
     * @param id ID of item to promote.
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Path("{id:.+}/up")
    public Response addPromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.upVote(item, currentUser);
            tx.success();
            return single(item);
        } catch (PromotionViews.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove an up vote.
     *
     * @param id ID of item to remove
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Path("{id:.+}/up")
    public Response removePromotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.removeUpVote(item, currentUser);
            tx.success();
            return single(item);
        }
    }

    /**
     * Down vote an item
     *
     * @param id ID of item to promote.
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Path("{id:.+}/down")
    public Response addDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.downVote(item, currentUser);
            tx.success();
            return single(item);
        } catch (PromotionViews.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Remove a down vote.
     *
     * @param id ID of item to remove
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Path("{id:.+}/down")
    public Response removeDemotion(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            pv.removeDownVote(item, currentUser);
            tx.success();
            return single(item);
        }
    }
}
