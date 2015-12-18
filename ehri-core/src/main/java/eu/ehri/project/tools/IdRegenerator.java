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

package eu.ehri.project.tools;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.VersionedEntity;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;

import java.util.Collection;
import java.util.List;

/**
 * Util class for re-generating the IDs for a given
 * set of items. Hierarchical IDs are used to enforce uniqueness
 * of identifiers within a particular parent-child scope. In
 * theory, since identifiers are generally very stable this works
 * fine, but occasionally they change, invalidating the identifiers
 * of their child items.
 * <p>
 * We also sometimes tweak the ID generation algorithm itself, which
 * necessitates bulk ID re-generation.
 */
public class IdRegenerator {
    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Serializer depSerializer;
    private final boolean dryrun;
    private final boolean skipCollisions;
    private final boolean collisionMode;

    public static class IdCollisionError extends Exception {
        public IdCollisionError(String from, String to) {
            super(String.format("Renaming %s to %s collide with existing item",
                    from, to));
        }
    }

    public IdRegenerator(FramedGraph<?> graph) {
        this(graph, true, false, false);
    }

    private IdRegenerator(FramedGraph<?> graph, boolean dryrun, boolean skipCollisions,
            boolean collisionMode) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.depSerializer = new Serializer.Builder(graph).dependentOnly().build();
        this.dryrun = dryrun;
        this.skipCollisions = skipCollisions;
        this.collisionMode = collisionMode;
    }

    public List<List<String>> reGenerateIds(PermissionScope scope, Iterable<? extends Frame> items) throws
            IdCollisionError {
        List<List<String>> remaps = Lists.newArrayList();
        for (Frame item: items) {
            Optional<List<String>> optionalRemap = reGenerateId(scope, item);
            if (optionalRemap.isPresent()) {
                remaps.add(optionalRemap.get());
            }
        }
        return remaps;
    }

    public List<List<String>> reGenerateIds(Iterable<? extends AccessibleEntity> items) throws IdCollisionError {
        List<List<String>> remaps = Lists.newArrayList();
        for (AccessibleEntity item: items) {
            Optional<List<String>> optionalRemap = reGenerateId(item);
            if (optionalRemap.isPresent()) {
                remaps.add(optionalRemap.get());
            }
        }
        return remaps;
    }

    public Optional<List<String>> reGenerateId(AccessibleEntity item) throws IdCollisionError {
        return reGenerateId(item.getPermissionScope(), item);
    }

    public Optional<List<String>> reGenerateId(PermissionScope permissionScope, Frame item)
            throws IdCollisionError {
        String currentId = item.getId();
        Collection<String> idChain = Lists.newArrayList();
        if (permissionScope != null && !permissionScope.equals(SystemScope.getInstance())) {
            idChain.addAll(permissionScope.idPath());
        }

        EntityClass entityClass = manager.getEntityClass(item);
        try {
            IdGenerator idgen = entityClass.getIdGen();
            Bundle itemBundle = depSerializer.vertexFrameToBundle(item);
            String newId = idgen.generateId(idChain, itemBundle);
            if (collisionMode) {
                if (!newId.equals(currentId) && manager.exists(newId)) {
                    List<String> collision = Lists.newArrayList(currentId, newId);
                    return Optional.of(collision);
                } else {
                    return Optional.absent();
                }
            } else {
                if (!newId.equals(currentId)) {
                    if (manager.exists(newId)) {
                        if (!skipCollisions) {
                            throw new IdCollisionError(currentId, newId);
                        } else {
                            return Optional.absent();
                        }
                    } else {
                        if (!dryrun) {
                            manager.renameVertex(item.asVertex(), currentId, newId);

                            // Rename all the descriptions
                            String idBase = idgen.getIdBase(itemBundle);
                            Collection<String> descIdChain = Lists.newArrayList(idChain);
                            descIdChain.add(idBase);
                            for (Description d : item.as(DescribedEntity.class).getDescriptions()) {
                                Bundle desc = depSerializer.vertexFrameToBundle(d);
                                String newDescriptionId = desc.getType().getIdGen().generateId(descIdChain, desc);
                                manager.renameVertex(d.asVertex(), d.getId(), newDescriptionId);
                            }

                            // Change the ID on any versions...
                            for (Version v : item.as(VersionedEntity.class).getAllPriorVersions()) {
                                manager.setProperty(v.asVertex(), Ontology.VERSION_ENTITY_ID, newId);
                            }
                        }
                        List<String> remap = Lists.newArrayList(currentId, newId);
                        return Optional.of(remap);
                    }
                } else {
                    return Optional.absent();
                }
            }
        } catch (SerializationError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtain a re-generator that will actually perform the rename
     * step.
     *
     * @param doIt whether to actually commit changes
     *
     * @return a new, more dangerous, re-generator
     */
    public IdRegenerator withActualRename(boolean doIt) {
        return new IdRegenerator(graph, !doIt, skipCollisions, collisionMode);
    }

    /**
     * Obtain a re-generator that will skip items that would collide
     * with existing items.
     *
     * @param skipCollisions whether or not to error on an ID collision
     *
     * @return a new, more tolerant, re-generator
     */
    public IdRegenerator skippingCollisions(boolean skipCollisions) {
        return new IdRegenerator(graph, dryrun, skipCollisions, collisionMode);
    }

    /**
     * Obtain a re-generator that only outputs items with IDs that if renamed
     * would collide with another item's ID.
     *
     * @param collisionMode only output items that would collide if renamed
     *
     * @return a new, more tolerant, re-generator
     */
    public IdRegenerator collisionMode(boolean collisionMode) {
        return new IdRegenerator(graph, dryrun, skipCollisions, collisionMode);
    }
}
