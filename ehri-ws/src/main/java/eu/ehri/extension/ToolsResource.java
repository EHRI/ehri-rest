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

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.errors.ConflictError;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.cvoc.SchemaExporter;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.idgen.DescriptionIdGenerator;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.tools.DbUpgrader1to2;
import eu.ehri.project.tools.FindReplace;
import eu.ehri.project.tools.IdRegenerator;
import eu.ehri.project.tools.Linker;
import eu.ehri.project.utils.Table;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * Miscellaneous additional functionality that doesn't
 * fit anywhere else.
 */
@Path(ToolsResource.ENDPOINT)
public class ToolsResource extends AbstractResource {

    private final Linker linker;

    public static final String ENDPOINT = "tools";

    private static final String SINGLE_PARAM = "single";
    private static final String ACCESS_POINT_TYPE_PARAM = "apt";

    public ToolsResource(@Context GraphDatabaseService database) {
        super(database);
        linker = new Linker(graph);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("version")
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    /**
     * Find and replace text in descriptions for a given item type
     * and description property name.
     * <p>
     * Changes will be logged to the audit log.
     *
     * @param type     the parent entity type
     * @param subType  the specific node type
     * @param property the property name
     * @param from     the original text
     * @param to       the replacement text
     * @param maxItems the max number of items to change
     *                 (defaults to 100)
     * @param commit   actually commit the changes
     * @return a list of item IDs for those items changed
     */
    @POST
    @Path("find-replace")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    public Table findReplace(
            final @FormParam("from") String from,
            final @FormParam("to") String to,
            final @QueryParam("type") String type,
            final @QueryParam("subtype") String subType,
            final @QueryParam("property") String property,
            final @QueryParam("max") @DefaultValue("100") int maxItems,
            final @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit) throws ValidationError {

        try {
            ContentTypes.withName(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid entity type (must be a content type)");
        }

        try (final Tx tx = beginTx()) {
            FindReplace fr = new FindReplace(graph, commit, maxItems);
            List<List<String>> rows = fr.findAndReplace(EntityClass.withName(type),
                    EntityClass.withName(subType), property, from, to,
                    getCurrentActioner(), getLogMessage().orElse(null));

            tx.success();
            return Table.of(rows);
        }
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
            final @QueryParam("baseUri") String baseUri) {
        final String rdfFormat = getRdfFormat(format);
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
            @QueryParam(ACCESS_POINT_TYPE_PARAM) Set<AccessPointType> accessPointTypes,
            @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String languageCode,
            @QueryParam(SINGLE_PARAM) @DefaultValue("true") boolean excludeSingle,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant)
            throws ItemNotFound, ValidationError, PermissionDenied {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
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
     * Regenerate the hierarchical graph ID for a set of items, optionally
     * renaming them.
     * <p>
     * The default mode is to output items whose IDs would change, without
     * actually changing them. The {@code collisions} parameter will <b>only</b>
     * output items that would cause collisions if renamed, whereas {@code tolerant}
     * mode will skip them altogether.
     * <p>
     * The {@code commit} flag will cause renaming to take place.
     *
     * @param ids        the existing item IDs
     * @param collisions only output items that if renamed to the
     *                   regenerated ID would cause collisions
     * @param tolerant   skip items that could cause collisions rather
     *                   then throwing an error
     * @param commit     whether or not to rename the item
     * @return a tab old-to-new mapping, or an empty
     * body if nothing was changed
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("regenerate-ids")
    public Table regenerateIds(
            @QueryParam("id") List<String> ids,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            Table data)
            throws ConflictError {
        try (final Tx tx = beginTx()) {
            List<String> allIds = Lists.newArrayList(ids);
            data.rows().stream()
                    .filter(row -> row.size() == 1)
                    .forEach(row -> allIds.add(row.get(0)));

            List<Accessible> items = allIds.stream().map(id -> {
                try {
                    return manager.getEntity(id, Accessible.class);
                } catch (ItemNotFound e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());

            List<List<String>> remap = new IdRegenerator(graph)
                    .withActualRename(commit)
                    .collisionMode(collisions)
                    .skippingCollisions(tolerant)
                    .reGenerateIds(items);
            tx.success();
            return Table.of(remap);
        } catch (IdRegenerator.IdCollisionError e) {
            throw new ConflictError(e);
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
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("regenerate-ids-for-type/{type:[^/]+}")
    public Table regenerateIdsForType(
            @PathParam("type") String type,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit)
            throws ConflictError {
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
                return Table.of(lists);
            }
        } catch (IdRegenerator.IdCollisionError e) {
            throw new ConflictError(e);
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
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("regenerate-ids-for-scope/{scope:[^/]+}")
    public Table regenerateIdsForScope(
            @PathParam("scope") String scopeId,
            @QueryParam("collisions") @DefaultValue("false") boolean collisions,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit)
            throws ItemNotFound, ConflictError {
        try (final Tx tx = beginTx()) {
            PermissionScope scope = manager.getEntity(scopeId, PermissionScope.class);
            List<List<String>> lists = new IdRegenerator(graph)
                    .withActualRename(commit)
                    .skippingCollisions(tolerant)
                    .collisionMode(collisions)
                    .reGenerateIds(scope.getAllContainedItems());
            tx.success();
            return Table.of(lists);
        } catch (IdRegenerator.IdCollisionError e) {
            throw new ConflictError(e);
        }
    }

    /**
     * Regenerate description IDs.
     */
    @POST
    @Produces("text/plain")
    @Path("regenerate-description-id/{id:[^/]+}")
    public String regenerateDescriptionId(@PathParam(ID_PARAM) String id,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit)
            throws ItemNotFound {
        int done = 0;
        try (final Tx tx = beginTx()) {
            final Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
            Described entity = manager.getEntity(id, Described.class);
            for (Description desc : entity.getDescriptions()) {
                PermissionScope scope = entity.getPermissionScope();
                List<String> idPath = scope != null
                        ? Lists.newArrayList(scope.idPath())
                        : Lists.newArrayList();
                idPath.add(entity.getIdentifier());
                Bundle descBundle = depSerializer.entityToBundle(desc);
                String newId = DescriptionIdGenerator.INSTANCE.generateId(idPath, descBundle);
                if (!newId.equals(desc.getId()) && commit) {
                    manager.renameVertex(desc.asVertex(), desc.getId(), newId);
                    done++;
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

    /**
     * Regenerate description IDs.
     */
    @POST
    @Produces("text/plain")
    @Path("regenerate-description-ids")
    public String regenerateDescriptionIds(
            @QueryParam("buffer") @DefaultValue("-1") int bufferSize,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit) {
        EntityClass[] types = {EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, EntityClass
                .CVOC_CONCEPT_DESCRIPTION, EntityClass.HISTORICAL_AGENT_DESCRIPTION, EntityClass
                .REPOSITORY_DESCRIPTION};
        int done = 0;
        try (final Tx tx = beginTx()) {
            final Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
            for (EntityClass entityClass : types) {
                try (CloseableIterable<Description> descriptions = manager.getEntities(entityClass, Description.class)) {
                    for (Description desc : descriptions) {
                        Described entity = desc.getEntity();
                        if (entity != null) {
                            PermissionScope scope = entity.getPermissionScope();
                            List<String> idPath = scope != null
                                    ? Lists.newArrayList(scope.idPath())
                                    : Lists.newArrayList();
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
    public String setLabels() {
        long done = 0;
        try (final Tx tx = beginTx()) {
            for (Vertex v : graph.getVertices()) {
                try {
                    ((Neo4jGraphManager<?>) manager).setLabels(v);
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
    public void fullUpgradeDb1to2() throws IOException {
        upgradeDb1to2();
        setLabels();
        setConstraints();
        try (Tx tx = beginTx()) {
            new DbUpgrader1to2(graph, () -> {
            }).setDbSchemaVersion();
            tx.success();
        }
    }

    /**
     * Takes a CSV file containing a mapping from one item to
     * another and moves changes the link target of anything linked
     * to <code>from</code> to <code>to</code>.
     *
     * @param mapping a comma-separated TSV file, excluding headers
     * @return CSV data with each row indicating the source, target, and how many items
     * were relinked for each
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("relink-targets")
    public Table relink(Table mapping) throws DeserializationError {
        try (final Tx tx = beginTx()) {
            List<List<String>> done = Lists.newArrayList();
            for (List<String> row : mapping.rows()) {
                if (row.size() != 2) {
                    throw new DeserializationError(
                            "Invalid table data: must contain 2 columns only");
                }
                String fromId = row.get(0);
                String toId = row.get(1);
                Linkable from = manager.getEntity(fromId, Linkable.class);
                Linkable to = manager.getEntity(toId, Linkable.class);
                int relinked = 0;
                for (Link link : from.getLinks()) {
                    link.addLinkTarget(to);
                    link.removeLinkTarget(from);
                    relinked++;
                }
                done.add(Lists.newArrayList(fromId, toId, String.valueOf(relinked)));
            }

            tx.success();
            return Table.of(done);
        } catch (ItemNotFound e) {
            throw new DeserializationError("Unable to locate item with ID: " + e.getValue());
        }
    }

    /**
     * Takes a CSV file containing two columns: an item global id, and a new parent
     * ID. The item will be re-parented and a new global ID regenerated.
     *
     * @param commit  actually commit changes
     * @param mapping a comma-separated CSV file, exluding headers.
     * @return CSV data containing two columns: the old global ID, and
     * a newly generated global ID, derived from the new hierarchy.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("reparent")
    public Table reparent(@QueryParam("commit") @DefaultValue("false") boolean commit, Table mapping)
            throws DeserializationError {
        try (final Tx tx = beginTx()) {
            IdRegenerator idRegenerator = new IdRegenerator(graph).withActualRename(commit);
            List<List<String>> done = Lists.newArrayList();
            for (List<String> row : mapping.rows()) {
                if (row.size() != 2) {
                    throw new DeserializationError(
                            "Invalid table data: must contain 2 columns only");
                }
                String id = row.get(0);
                String newParentId = row.get(1);
                DocumentaryUnit item = manager
                        .getEntity(id, EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class);
                PermissionScope parent = manager.getEntity(newParentId, PermissionScope.class);
                item.setPermissionScope(parent);
                if (Entities.DOCUMENTARY_UNIT.equals(parent.getType())) {
                    parent.as(DocumentaryUnit.class).addChild(item);
                } else if (Entities.REPOSITORY.equals(parent.getType())) {
                    item.setRepository(parent.as(Repository.class));
                } else {
                    throw new DeserializationError(String.format(
                            "Unsupported parent type for ID '%s': %s",
                            newParentId, parent.getType()));
                }
                try {
                    idRegenerator.reGenerateId(item).ifPresent(done::add);
                } catch (IdRegenerator.IdCollisionError e) {
                    throw new DeserializationError(String.format(
                            "%s. Ensure they do not share the same local identifier: '%s'",
                            e.getMessage(), item.getIdentifier()));
                }
            }

            if (commit) {
                tx.success();
            }
            return Table.of(done);
        } catch (ItemNotFound e) {
            throw new DeserializationError("Unable to locate item with ID: " + e.getValue());
        }
    }

    /**
     * Takes a CSV file containing two columns: the global id, and a new local
     * identifier to rename an item to. A new global ID will be regenerated.
     * <p>
     * Input rows will be re-sorted lexically to ensure correct generation
     * of dependent parent/child hierarchical IDs and output order will
     * reflect this.
     *
     * @param commit actually perform the rename
     * @param mapping a comma-separated CSV file, excluding headers.
     * @return CSV data containing two columns: the old global ID, and
     * a newly generated global ID, derived from the new local identifier,
     * with ordering corresponding to lexically-ordered input data.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("rename")
    public Table rename(@QueryParam("commit") @DefaultValue("false") boolean commit, Table mapping)
            throws IdRegenerator.IdCollisionError, DeserializationError {
        try (final Tx tx = beginTx()) {
            IdRegenerator idRegenerator = new IdRegenerator(graph).withActualRename(commit);
            // Sorting the toString of each item *should* put hierarchical lists
            // in parent-child order, which is necessary to have ID-regeneration work
            // correctly.
            List<List<String>> sorted = Ordering.usingToString().sortedCopy(mapping.rows());

            List<List<String>> done = Lists.newArrayList();
            for (List<String> row : sorted) {
                if (row.size() != 2) {
                    throw new DeserializationError(
                            "Invalid table data: must contain 2 columns only");
                }
                String currentId = row.get(0);
                String newLocalIdentifier = row.get(1);
                Accessible item = manager.getEntity(currentId, Accessible.class);
                item.asVertex().setProperty(Ontology.IDENTIFIER_KEY, newLocalIdentifier);
                idRegenerator.reGenerateId(item).ifPresent(done::add);
            }

            if (commit) {
                tx.success();
            }
            return Table.of(done);
        } catch (ItemNotFound e) {
            throw new DeserializationError("Unable to locate item with ID: " + e.getValue());
        }
    }

    /**
     * Extremely lossy helper method for cleaning a test instance
     *
     * @param fixtures YAML fixture data to be loaded into the fresh graph
     */
    @POST
    @Path("__INITIALISE")
    public void initialize(
            @QueryParam("yes-i-am-sure") @DefaultValue("false") boolean confirm, InputStream fixtures) {
        try (final Tx tx = beginTx()) {
            sanityCheck(confirm);

            for (Vertex v : graph.getVertices()) {
                v.remove();
            }
            tx.success();
        }
        setConstraints();
        try (final Tx tx = beginTx()) {
            FixtureLoaderFactory.getInstance(graph, true)
                    .loadTestData(fixtures);
            tx.success();
        }
    }

    private void sanityCheck(boolean confirm) {
        // Bail out if we've got many nodes
        Iterator<Vertex> counter = graph.getVertices().iterator();
        int c = 0;
        while (counter.hasNext()) {
            counter.next();
            c++;
            if (c > 500) {
                if (!confirm) {
                    throw new RuntimeException("This database has more than 500 nodes. " +
                            "Refusing to clear it without confirmation!");
                } else break;
            }
        }
    }
}
