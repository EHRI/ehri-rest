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
import eu.ehri.extension.base.ParentResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.core.Tx;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
 * Provides a RESTful interface for the Repository.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.REPOSITORY)
public class RepositoryResource extends AbstractAccessibleEntityResource<Repository>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource {

    public RepositoryResource(@Context GraphDatabaseService database) {
        super(database, Repository.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound,
            BadRequester {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response list() throws BadRequester {
        return listItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    @Override
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Repository repository = views.detail(id, user);
            Iterable<DocumentaryUnit> units = all
                    ? repository.getAllCollections()
                    : repository.getCollections();
            return streamingPage(getQuery(DocumentaryUnit.class).page(units, user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
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
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response response = deleteItem(id);
            tx.success();
            return response;
        }
    }

    /**
     * Create a documentary unit for this repository.
     * 
     * @param id The repository ID
     * @param bundle The new unit data
     * @return The new unit
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    @Override
    public Response createChild(@PathParam("id") String id,
                                Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            final Repository repository = views.detail(id, user);
            Response response = createItem(bundle, accessors, new Handler<DocumentaryUnit>() {
                @Override
                public void process(DocumentaryUnit doc) throws PermissionDenied {
                    repository.addCollection(doc);
                }
            }, views.setScope(repository).setClass(DocumentaryUnit.class));
            tx.success();
            return response;
        }
    }
}
