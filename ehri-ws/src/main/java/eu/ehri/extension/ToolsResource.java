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

import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.exporters.cvoc.SchemaExporter;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.tools.DbUpgrader1to2;
import eu.ehri.project.tools.IdRegenerator;
import eu.ehri.project.tools.Linker;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Miscellaneous additional functionality that doesn't
 * fit anywhere else.
 */
@Path(ToolsResource.ENDPOINT)
public class ToolsResource extends AbstractResource {

    private final Linker linker;

    public static final String ENDPOINT = "tools";
    public static final String TOLERANT_PARAM = "tolerant";
    public static final String SINGLE_PARAM = "single";
    public static final String LANG_PARAM = "lang";
    public static final String ACCESS_POINT_TYPE_PARAM = "apt";
    public static final String DEFAULT_LANG = "eng";

    public ToolsResource(@Context GraphDatabaseService database) {
        super(database);
        linker = new Linker(graph);
    }

    /**
     * Export an RDFS+OWL representation of the model schema.
     *
     * @param format  the RDF format
     * @param baseUri the RDF base URI
     * @return a streaming response
     */
    @GET
    @Path("schema")
    @Produces({TURTLE_MIMETYPE, RDF_XML_MIMETYPE, N3_MIMETYPE})
    public Response exportSchema(
            final @QueryParam("format") String format,
            final @QueryParam("baseUri") String baseUri) throws IOException {
        final String rdfFormat = getRdfFormat(format, "TTL");
        final MediaType mediaType = MediaType.valueOf(RDF_MIMETYPE_FORMATS
                .inverse().get(rdfFormat));
        final SchemaExporter schemaExporter = new SchemaExporter(rdfFormat);
        return Response.ok((StreamingOutput) outputStream ->
                schemaExporter.dumpSchema(outputStream, baseUri))
                .type(mediaType + "; charset=utf-8").build();
    }

    /**
     * Create concepts and links derived from the access points
     * on a repository's documentary units.
     *
     * @param repositoryId     the repository id
     * @param vocabularyId     the target vocabulary
     * @param languageCode     the language code of created concepts
     * @param accessPointTypes the access point types to process
     * @param tolerant         proceed even if there are integrity errors due
     *                         to slug collisions in the created concepts
     * @param excludeSingle    don't create concepts/links for access points that
     *                         are unique to a single item
     * @return the number of links created
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("generate-concepts/{repositoryId:[^/]+}/{vocabularyId:[^/]+}")
    public long autoLinkRepositoryDocs(
            @PathParam("repositoryId") String repositoryId,
            @PathParam("vocabularyId") String vocabularyId,
            @QueryParam(ACCESS_POINT_TYPE_PARAM) List<String> accessPointTypes,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String languageCode,
            @QueryParam(SINGLE_PARAM) @DefaultValue("true") boolean excludeSingle,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant)
            throws ItemNotFound, ValidationError,
            PermissionDenied, DeserializationError {
        try (final Tx tx = beginTx()) {
            UserProfile user = getCurrentUser();
            Repository repository = manager.getEntity(repositoryId, Repository.class);
            Vocabulary vocabulary = manager.getEntity(vocabularyId, Vocabulary.class);

            long linkCount = linker
                    .withAccessPointTypes(accessPointTypes)
                    .withTolerant(tolerant)
                    .withExcludeSingles(excludeSingle)
                    .withDefaultLanguage(languageCode)
                    .withLogMessage(getLogMessage())
                    .createAndLinkRepositoryVocabulary(repository, vocabulary, user);

            tx.success();
            return linkCount;
        }
    }

    /**
     * Regenerate the hierarchical graph ID for a given item, optionally
     * renaming it.
     * <p>
     * The default mode is to output items whose IDs would change, without
     * actually changing them. The {@code collisions} parameter will <b>only</b>
     * output items that would cause collisions if renamed, whereas {@code tolerant}
     * mode will skip them altogether.
     * <p>
     * The {@code commit} flag will cause renaming to take place.
     *
     * @param id         the item's existing ID
     * @param collisions only output items that if renamed to the
     *                   regenerated ID would cause collisions
     * @param tolerant   skip items that could cause collisions rather
     *                   then throwing an error
     * @param commit     whether or not to rename the item
     * @return a tab old-to-new mapping, or an empty
     * body if nothing was changed
     */
    @POST
    @Produces("text/csv")
    @Path("regenerate-id/{id:[^/]+}")
    public String regenerateId(
            @PathParam("id") String id,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam("tolerant") @DefaultValue("false") boolean tolerant,
            @QueryParam("commit") @DefaultValue("false") boolean commit)
            throws ItemNotFound, IOException, IdRegenerator.IdCollisionError {
        try (final Tx tx = beginTx()) {
            Accessible item = manager.getEntity(id, Accessible.class);
            Optional<List<String>> remap = new IdRegenerator(graph)
                    .withActualRename(commit)
                    .collisionMode(collisions)
                    .skippingCollisions(tolerant)
                    .reGenerateId(item);
            tx.success();
            return makeCsv(Lists.newArrayList(remap.asSet()));
        }
    }

    /**
     * Regenerate the hierarchical graph ID all items of a given
     * type.
     * <p>
     * The default mode is to output items whose IDs would change, without
     * actually changing them. The {@code collisions} parameter will <b>only</b>
     * output items that would cause collisions if renamed, whereas {@code tolerant}
     * mode will skip them altogether.
     * <p>
     * The {@code commit} flag will cause renaming to take place.
     *
     * @param type       the item type
     * @param collisions only output items that if renamed to the
     *                   regenerated ID would cause collisions
     * @param tolerant   skip items that could cause collisions rather
     *                   then throwing an error
     * @param commit     whether or not to rename the items
     * @return a tab list old-to-new mappings, or an empty
     * body if nothing was changed
     */
    @POST
    @Produces("text/csv")
    @Path("regenerate-ids-for-type/{type:[^/]+}")
    public String regenerateIdsForType(
            @PathParam("type") String type,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam("tolerant") @DefaultValue("false") boolean tolerant,
            @QueryParam("commit") @DefaultValue("false") boolean commit)
            throws IOException, IdRegenerator.IdCollisionError {
        try (final Tx tx = beginTx()) {
            EntityClass entityClass = EntityClass.withName(type);
            try (CloseableIterable<Accessible> frames = manager
                    .getEntities(entityClass, Accessible.class)) {
                List<List<String>> lists = new IdRegenerator(graph)
                        .withActualRename(commit)
                        .collisionMode(collisions)
                        .skippingCollisions(tolerant)
                        .reGenerateIds(frames);
                tx.success();
                return makeCsv(lists);
            }
        }
    }

    /**
     * Regenerate the hierarchical graph ID for all items within the
     * permission scope and lower levels.
     * <p>
     * The default mode is to output items whose IDs would change, without
     * actually changing them. The {@code collisions} parameter will <b>only</b>
     * output items that would cause collisions if renamed, whereas {@code tolerant}
     * mode will skip them altogether.
     * <p>
     * The {@code commit} flag will cause renaming to take place.
     *
     * @param scopeId    the scope item's ID
     * @param collisions only output items that if renamed to the
     *                   regenerated ID would cause collisions
     * @param tolerant   skip items that could cause collisions rather
     *                   then throwing an error
     * @param commit     whether or not to rename the items
     * @return a tab list old-to-new mappings, or an empty
     * body if nothing was changed
     */
    @POST
    @Produces("text/csv")
    @Path("regenerate-ids-for-scope/{scope:[^/]+}")
    public String regenerateIdsForScope(
            @PathParam("scope") String scopeId,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam("tolerant") @DefaultValue("false") boolean tolerant,
            @QueryParam("commit") @DefaultValue("false") boolean commit)
            throws IOException, ItemNotFound, IdRegenerator.IdCollisionError {
        try (final Tx tx = beginTx()) {
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);
            List<List<String>> lists = new IdRegenerator(graph)
                    .withActualRename(commit)
                    .skippingCollisions(tolerant)
                    .collisionMode(collisions)
                    .reGenerateIds(scope.getAllContainedItems());
            tx.success();
            return makeCsv(lists);
        }
    }

    /**
     * Regenerate description IDs.
     *
     */
    @POST
    @Produces("text/plain")
    @Path("regenerate-description-ids")
    public String regenerateDescriptionIds(
            @QueryParam("buffer") @DefaultValue("-1") int bufferSize,
            @QueryParam("commit") @DefaultValue("false") boolean commit)
            throws IOException, ItemNotFound, IdRegenerator.IdCollisionError {
        EntityClass[] types = {EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, EntityClass
                .CVOC_CONCEPT_DESCRIPTION, EntityClass.HISTORICAL_AGENT_DESCRIPTION, EntityClass
                .REPOSITORY_DESCRIPTION};
        Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
        int done = 0;
        try (final Tx tx = beginTx()) {
            for (EntityClass entityClass : types) {
                try (CloseableIterable<Description> descriptions = manager.getEntities(entityClass, Description.class)) {
                    for (Description desc : descriptions) {
                        Described entity = desc.getEntity();
                        if (entity != null) {
                            PermissionScope scope = entity.getPermissionScope();
                            List<String> idPath = scope != null
                                    ? Lists.newArrayList(scope.idPath())
                                    : Lists.<String>newArrayList();
                            idPath.add(entity.getIdentifier());
                            Bundle descBundle = depSerializer.entityToBundle(desc);
                            String newId = entityClass.getIdGen().generateId(idPath, descBundle);
                            if (!newId.equals(desc.getId()) && commit) {
                                manager.renameVertex(desc.asVertex(), desc.getId(), newId);
                                done++;

                                if (bufferSize > 0 && done % bufferSize == 0) {
                                    tx.success();
                                }
                            }
                        }
                    }
                }
            }
            if (commit && done > 0) {
                tx.success();
            }
            return String.valueOf(done);
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Produces("text/plain")
    @Path("set-labels")
    public String setLabels()
            throws IOException, ItemNotFound, IdRegenerator.IdCollisionError {
        long done = 0;
        try (final Tx tx = beginTx()) {
            for (Vertex v : graph.getVertices()) {
                try {
                    ((Neo4jGraphManager) manager).setLabels(v);
                    done++;
                } catch (org.neo4j.graphdb.ConstraintViolationException e) {
                    logger.error("Error setting labels on {} ({})", manager.getId(v), v.getId());
                    e.printStackTrace();
                }

                if (done % 100000 == 0) {
                    graph.getBaseGraph().commit();
                }
            }
            tx.success();
        }

        return String.valueOf(done);
    }

    @POST
    @Produces("text/plain")
    @Path("set-constraints")
    public void setConstraints() {
        try (final Tx tx = beginTx()) {
            logger.info("Initializing graph schema...");
            manager.initialize();
            tx.success();
        }
    }

    @POST
    @Produces("text/plain")
    @Path("upgrade-1to2")
    public String upgradeDb1to2() throws IOException {
        final AtomicInteger done = new AtomicInteger();
        try (final Tx tx = beginTx()) {
            logger.info("Upgrading DB schema...");
            DbUpgrader1to2 upgrader1to2 = new DbUpgrader1to2(graph, () -> {
                if (done.getAndIncrement() % 100000 == 0) {
                    graph.getBaseGraph().commit();
                }
            });
            upgrader1to2
                    .upgradeIdAndTypeKeys()
                    .upgradeTypeValues()
                    .setIdAndTypeOnEventLinks();
            tx.success();
            logger.info("Changed {} items", done.get());
            return String.valueOf(done.get());
        }
    }

    @POST
    @Produces("text/plain")
    @Path("full-upgrade-1to2")
    public void fullUpgradeDb1to2()
            throws IOException, IdRegenerator.IdCollisionError, ItemNotFound {
        upgradeDb1to2();
        setLabels();
        setConstraints();
        try (Tx tx = beginTx()) {
            new DbUpgrader1to2(graph, () -> {
            }).setDbSchemaVersion();
            tx.success();
        }
    }


    // Helpers

    private String makeCsv(List<List<String>> rows) throws IOException {
        CsvMapper mapper = new CsvMapper()
                .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING);
        CsvSchema schema = mapper.schemaFor(List.class).withoutHeader();
        return mapper.writer(schema).writeValueAsString(rows);
    }
}
