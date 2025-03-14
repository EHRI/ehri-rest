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

package eu.ehri.project.ws;

import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.cvoc.JenaSkosExporter;
import eu.ehri.project.importers.cvoc.SkosRDFVocabulary;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFWriter;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Provides a web service interface for the Vocabulary model. Vocabularies are
 * containers for Concepts.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.CVOC_VOCABULARY)
public class VocabularyResource extends AbstractAccessibleResource<Vocabulary>
        implements GetResource, ListResource, DeleteResource, CreateResource, UpdateResource, ParentResource {

    public VocabularyResource(@Context GraphDatabaseService database) {
        super(database, Vocabulary.class);
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/list")
    @Override
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response page = streamingPage(() -> {
                Vocabulary vocabulary = manager.getEntityUnchecked(id, cls);
                return getQuery()
                        .page(all ? vocabulary.getConcepts() : vocabulary.getTopConcepts(), Concept.class);
            });
            tx.success();
            return page;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response create(Bundle bundle,
                           @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError {
        try (final Tx tx = beginTx()) {
            Response item = createItem(bundle, accessors);
            tx.success();
            return item;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
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

    @DELETE
    @Path("{id:[^/]+}/list")
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Override
    public Table deleteChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all,
            @QueryParam(VERSION_PARAM) @DefaultValue("true") boolean version,
            @QueryParam("batch") @DefaultValue("-1") int batchSize)
                throws ItemNotFound, PermissionDenied, HierarchyError {
        try (final Tx tx = beginTx()) {
            Table out = deleteContents(id, all, version, batchSize);
            tx.success();
            return out;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response createChild(@PathParam("id") String id,
                                @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Vocabulary vocabulary = api().get(id, cls);
            Response item = createItem(bundle, accessors,
                    concept -> concept.setVocabulary(vocabulary),
                    api().withScope(vocabulary), Concept.class);
            tx.success();
            return item;
        }
    }

    /**
     * Export the given vocabulary as SKOS.
     *
     * @param id      the vocabulary id
     * @param format  the RDF format. Can be one of: RDF/XML, N3, TTL
     * @param baseUri the base URI for exported items
     * @return a SKOS vocabulary
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/export")
    @Produces({TURTLE_MIMETYPE, RDF_XML_MIMETYPE, N3_MIMETYPE})
    public Response exportSkos(@PathParam("id") String id,
                               final @QueryParam("format") String format,
                               final @QueryParam("baseUri") String baseUri)
            throws ItemNotFound {
        final String rdfFormat = getRdfFormat(format);
        final String base = baseUri == null ? SkosRDFVocabulary.DEFAULT_BASE_URI : baseUri;
        final MediaType mediaType = MediaType.valueOf(RDF_MIMETYPE_FORMATS
                .inverse().get(rdfFormat));
        try (final Tx tx = beginTx()) {
            final Vocabulary vocabulary = api().get(id, cls);
            final JenaSkosExporter skosImporter = new JenaSkosExporter(graph, vocabulary);
            final Model model = skosImporter.export(base);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                RDFWriter writer = model.getWriter(rdfFormat);
                writer.setProperty("relativeURIs", "");
                writer.write(model, outputStream, base);
            }).type(mediaType + "; charset=utf-8").build();
        }
    }
}
