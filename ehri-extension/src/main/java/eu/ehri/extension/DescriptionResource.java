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
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.core.Tx;
import eu.ehri.project.views.DescriptionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful interface for dealing with described entities.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(DescriptionResource.ENDPOINT)
public class DescriptionResource extends AbstractAccessibleEntityResource<DescribedEntity> {

    public static final String ENDPOINT = "description";

    private final DescriptionViews<DescribedEntity> descriptionViews;

    public DescriptionResource(@Context GraphDatabaseService database) {
        super(database, DescribedEntity.class);
            descriptionViews = new DescriptionViews<>(graph, DescribedEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response createDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
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
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor userProfile = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, userProfile);
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
    @Path("/{id:.+}/{did:.+}")
    public Response updateDescriptionWithId(@PathParam("id") String id,
            @PathParam("did") String did, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        return updateDescription(id, bundle.withId(did));
    }

    @DELETE
    @Path("/{id:.+}/{did:.+}")
    public Response deleteDescription(
            @PathParam("id") String id, @PathParam("did") String did)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
            descriptionViews.delete(id, did, user, getLogMessage());
            Response response = Response.ok().location(getItemUri(item)).build();
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }


    private Response buildResponse(DescribedEntity item, Frame data, Response.Status status)
            throws SerializationError {
        return Response.status(status).location(getItemUri(item))
                .entity((getSerializer().vertexFrameToJson(data))
                        .getBytes(Charsets.UTF_8)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP)
    public Response createAccessPoint(@PathParam("id") String id,
                @PathParam("did") String did, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
            Description desc = manager.getFrame(did, Description.class);
            UndeterminedRelationship rel = descriptionViews.create(id, bundle,
                    UndeterminedRelationship.class, user, getLogMessage());
            desc.addUndeterminedRelationship(rel);
            Response response = buildResponse(item, rel, Response.Status.CREATED);
            tx.success();
            return response;
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    @SuppressWarnings("unused")
    @DELETE
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP + "/{apid:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, @PathParam("apid") String apid)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
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
