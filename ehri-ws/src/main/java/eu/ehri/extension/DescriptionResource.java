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

import com.google.common.base.Charsets;
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.DescriptionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a web service interface for dealing with described entities.
 */
@Path(DescriptionResource.ENDPOINT)
public class DescriptionResource extends AbstractAccessibleResource<Described> {

    public static final String ENDPOINT = "description";

    private final DescriptionViews<Described> descriptionViews;

    public DescriptionResource(@Context GraphDatabaseService database) {
        super(database, Described.class);
        descriptionViews = new DescriptionViews<>(graph, Described.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response createDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Described item = views.detail(id, user);
            Description desc = descriptionViews.create(id, bundle,
                    Description.class, user, getLogMessage());
            item.addDescription(desc);
            Response response = buildResponse(item, desc, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response updateDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor userProfile = getRequesterUserProfile();
            Described item = views.detail(id, userProfile);
            Mutation<Description> desc = descriptionViews.update(id, bundle,
                    Description.class, userProfile, getLogMessage());
            Response response = buildResponse(item, desc.getNode(), Response.Status.OK);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/{did:.+}")
    public Response updateDescriptionWithId(@PathParam("id") String id,
                                            @PathParam("did") String did, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, SerializationError {
        return updateDescription(id, bundle.withId(did));
    }

    @DELETE
    @Path("{id:.+}/{did:.+}")
    public Response deleteDescription(
            @PathParam("id") String id, @PathParam("did") String did)
            throws PermissionDenied, ItemNotFound, ValidationError, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Described item = views.detail(id, user);
            descriptionViews.delete(id, did, user, getLogMessage());
            Response response = Response.ok().location(getItemUri(item)).build();
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }


    private Response buildResponse(Described item, Entity data, Response.Status status)
            throws SerializationError {
        return Response.status(status).location(getItemUri(item))
                .entity((getSerializer().entityToJson(data))
                        .getBytes(Charsets.UTF_8)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/{did:.+}/" + Entities.ACCESS_POINT)
    public Response createAccessPoint(@PathParam("id") String id,
                                      @PathParam("did") String did, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Described item = views.detail(id, user);
            Description desc = manager.getEntity(did, Description.class);
            AccessPoint rel = descriptionViews.create(id, bundle,
                    AccessPoint.class, user, getLogMessage());
            desc.addAccessPoint(rel);
            Response response = buildResponse(item, rel, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    @SuppressWarnings("unused")
    @DELETE
    @Path("{id:.+}/{did:.+}/" + Entities.ACCESS_POINT + "/{apid:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id,
                                      @PathParam("did") String did, @PathParam("apid") String apid)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            UserProfile user = getCurrentUser();
            descriptionViews.delete(id, apid, user, getLogMessage());
            tx.success();
            return Response.ok().build();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }
}
