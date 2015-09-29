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

import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.AnnotationViews;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.impl.CrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Web service interface for creating annotations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.ANNOTATION)
public class AnnotationResource
        extends AbstractAccessibleEntityResource<Annotation>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    private final AnnotationViews annotationViews;

    public AnnotationResource(@Context GraphDatabaseService database) {
        super(database, Annotation.class);
        annotationViews = new AnnotationViews(graph);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response list() {
        return listItems();
    }

    /**
     * Create an annotation for a particular item.
     *
     * @param id        the ID of the item being annotation
     * @param bundle    the JSON representation of the annotation
     * @param accessors user IDs who can access the annotation
     * @return the annotation
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    public Response createAnnotationFor(@PathParam("id") String id,
                                        Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, AccessDenied, ValidationError, DeserializationError,
            ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Annotation ann = annotationViews.createFor(id, id,
                    bundle, user, getAccessors(accessors, user));
            Response response = creationResponse(ann);
            tx.success();
            return response;
        }
    }

    /**
     * Create an annotation for a dependent node on a given item.
     *
     * @param id        the ID of the item being annotation
     * @param did       the ID of the description being annotated
     * @param bundle    the JSON representation of the annotation
     * @param accessors user IDs who can access the annotation
     * @return the annotation
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws SerializationError
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/{did:.+}")
    public Response createAnnotationFor(
            @PathParam("id") String id,
            @PathParam("did") String did,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, AccessDenied, ValidationError, DeserializationError,
            ItemNotFound, SerializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            Annotation ann = annotationViews.createFor(id, did,
                    bundle, user, getAccessors(accessors, user));
            Response response = creationResponse(ann);
            tx.success();
            return response;
        }
    }

    /**
     * Return a map of annotations for the subtree of the given item and its
     * child items. Standard list parameters apply.
     *
     * @param id the item ID
     * @return a list of annotations on the item and it's dependent children.
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public Response listAnnotationsForSubtree(
            @PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            AccessibleEntity item = new CrudViews<>(graph, AccessibleEntity.class)
                    .detail(id, getRequesterUserProfile());
            Query<Annotation> query = getQuery(Annotation.class)
                    .setStream(isStreaming());
            return streamingPage(query.page(item.getAnnotations(), getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @PUT
    @Path("{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ItemNotFound, ValidationError, DeserializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response response = updateItem(id, bundle);
            tx.success();
            return response;
        }
    }

    @DELETE
    @Path("{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response response = deleteItem(id);
            tx.success();
            return response;
        }
    }
}
