/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.links;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ApiFactory;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.MutationState;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class LinkResolver {

    private static final Logger logger = LoggerFactory.getLogger(LinkResolver.class);
    private static final Config config = ConfigFactory.load();

    // Property keys an access point needs to be resolvable to a vocabulary/authority item.
    public static final String CVOC = "cvoc";
    public static final String CONCEPT = "concept";

    private final GraphManager manager;
    private final Api api;
    private final Serializer mergeSerializer;
    private final PreImportCallback callback;

    private final Bundle linkTemplate = Bundle.of(EntityClass.LINK)
            // TODO: allow overriding link type and text here
            .withDataValue(Ontology.LINK_HAS_DESCRIPTION, config.getString("io.import.defaultLinkText"))
            .withDataValue(Ontology.LINK_HAS_TYPE, config.getString("io.import.defaultLinkType"));

    private final LoadingCache<String, AuthoritativeSet> setCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, AuthoritativeSet>() {
                @Override
                public AuthoritativeSet load(String key) throws Exception {
                    return manager.getEntity(key, AuthoritativeSet.class);
                }
            });

    public LinkResolver(FramedGraph<?> graph, Accessor accessor, PreImportCallback callback) {
        api = ApiFactory.noLogging(graph, accessor);
        manager = GraphManagerFactory.getInstance(graph);
        mergeSerializer = new Serializer.Builder(graph).dependentOnly().build();
        this.callback = callback;
    }

    public static LinkResolver create(FramedGraph<?> graph, Accessor accessor, PreImportCallback callback) {
        return new LinkResolver(graph, accessor, callback);
    }

    /**
     * Resolve the access points of a mutation's item, promoting its state to
     * UPDATED if links were created but it was otherwise unchanged.
     *
     * @param mutation the mutation whose item's access points should be resolved
     * @return the (possibly promoted) mutation
     */
    public <T extends Described> Mutation<T> resolveLinks(Mutation<T> mutation) throws ValidationError {
        int created = solveUndeterminedRelationships(mutation.getNode());
        return created > 0 && mutation.getState() == MutationState.UNCHANGED
                ? Mutation.updated(mutation.getNode())
                : mutation;
    }

    /**
     * Resolve a unit's access points that carry cvoc/concept (or target) attributes
     * into links against the matching item in that vocabulary/authority set.
     *
     * @param unit the described item whose access points should be resolved
     * @return the number of new links created
     */
    public int solveUndeterminedRelationships(Described unit) throws ValidationError {
        logger.debug("Resolving relationships for {}", unit.getId());
        int created = 0;
        for (Description desc : unit.getDescriptions()) {
            // Use a Set to avoid resolving the same access point twice.
            for (AccessPoint accessPoint : Sets.newHashSet(desc.getAccessPoints())) {
                if (resolveAccessPoint(unit, accessPoint)) {
                    created++;
                }
            }
        }
        return created;
    }

    /**
     * Resolve a single access point to a link, creating one if it doesn't already exist.
     *
     * @return true if a new link was created
     */
    private boolean resolveAccessPoint(Described unit, AccessPoint accessPoint) throws ValidationError {
        Set<String> keys = accessPoint.getPropertyKeys();
        if (!keys.contains(CVOC) || (!keys.contains(CONCEPT) && !keys.contains(ImportHelpers.LINK_TARGET))) {
            return false;
        }

        String setId = accessPoint.getProperty(CVOC);
        String targetId = accessPoint.getProperty(CONCEPT);
        if (targetId == null) {
            targetId = accessPoint.getProperty(ImportHelpers.LINK_TARGET);
        }
        logger.debug(" - found link references: cvoc: {}, concept: {}", setId, targetId);

        AuthoritativeSet set;
        try {
            set = setCache.get(setId);
        } catch (ExecutionException e) {
            logger.warn(" - unable to find link set with id: {}", setId);
            return false;
        }

        Optional<AuthoritativeItem> targetOpt = findTarget(set, targetId);
        if (!targetOpt.isPresent()) {
            logger.warn(" - unable to find link target with id: {}", targetId);
            return false;
        }
        AuthoritativeItem target = targetOpt.get();

        try {
            if (findLink(unit, target, accessPoint, linkTemplate).isPresent()) {
                logger.debug(" - found existing link between {} and {}", targetId, target.getId());
                return false;
            }
            Link link = api.create(callback.preImport(Collections.emptyList(), linkTemplate), Link.class);
            unit.addLink(link);
            target.addLink(link);
            link.addLinkBody(accessPoint);
            logger.debug(" - new link created between {} and {}", targetId, target.getId());
            return true;
        } catch (PermissionDenied | DeserializationError | SerializationError e) {
            logger.error("Unexpected error resolving link for {}/{}", setId, targetId, e);
            return false;
        }
    }

    private Optional<AuthoritativeItem> findTarget(AuthoritativeSet set, String itemId) {
        for (AuthoritativeItem item : set.getAuthoritativeItems()) {
            if (Objects.equals(item.getIdentifier(), itemId)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private Optional<Link> findLink(Described unit, Linkable target, AccessPoint body, Bundle data)
            throws SerializationError {
        for (Link link : unit.getLinks()) {
            for (Linkable connected : link.getLinkTargets()) {
                if (target.equals(connected)
                        && Iterables.contains(link.getLinkBodies(), body)
                        && mergeSerializer.entityToBundle(link).equals(data)) {
                    return Optional.of(link);
                }
            }
        }
        return Optional.empty();
    }
}
