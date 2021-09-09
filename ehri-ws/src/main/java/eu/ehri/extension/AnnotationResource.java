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

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Web service interface for creating annotations.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.ANNOTATION)
public class AnnotationResource extends AbstractAccessibleResource<Annotation>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String TARGET_PARAM = "target";
    public static final String BODY_PARAM = "body";

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response list() {
        return listItems();
    }

    /**
     * Create an annotation for a particular item.
     *
     * @param id        the ID of the item being annotation
     * @param did       the (optional) ID of the sub-item target, e.g. a description
     * @param bundle    the JSON representation of the annotation
     * @param accessors user IDs who can access the annotation
     * @return the annotation
     * @throws AccessDenied         if the user cannot access the item
     * @throws ItemNotFound         if the parent does not exist
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if data constraints are not met
     * @throws DeserializationError if the input data is not valid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAnnotation(
            @QueryParam(TARGET_PARAM) String id,
            @QueryParam(BODY_PARAM) String did,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors,
            Bundle bundle)
            throws PermissionDenied, AccessDenied, ValidationError, DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            if (id == null) {
                throw new DeserializationError("Target must be provided");
            }
            UserProfile user = getCurrentUser();
            Annotation ann = api().createAnnotation(id, did == null ? id : did,
                    bundle, getAccessors(accessors, user), getLogMessage());
            Response response = creationResponse(ann);
            tx.success();
            return response;
        }
    }

    @PUT
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ItemNotFound, ValidationError, DeserializationError {
        try (final Tx tx = beginTx()) {
            Response response = updateItem(id, bundle);
            tx.success();
            return response;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }
}
