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
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class LinkResolver {

    private final GraphManager manager;
    private final Api api;
    private final Serializer mergeSerializer;

    private static final Logger logger = LoggerFactory.getLogger(LinkResolver.class);
    private static final Config config = ConfigFactory.load();

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

    public LinkResolver(FramedGraph<?> graph, Accessor accessor) {
        api = ApiFactory.noLogging(graph, accessor);
        manager = GraphManagerFactory.getInstance(graph);
        mergeSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }


    public int solveUndeterminedRelationships(Described unit) throws ValidationError {
        logger.debug("Resolving relationships for {}", unit.getId());
        int created = 0;

        for (Description desc : unit.getDescriptions()) {
            // Put the set of relationships into a HashSet to remove duplicates.
            for (AccessPoint rel : Sets.newHashSet(desc.getAccessPoints())) {
                // the wp2 undetermined relationship that can be resolved have a 'cvoc' and a 'concept' attribute.
                // they need to be found in the vocabularies that are in the graph
                Set<String> relDataKeys = rel.getPropertyKeys();
                if (relDataKeys.contains("cvoc")
                        && (relDataKeys.contains("concept") || relDataKeys.contains("target"))) {
                    String setId = rel.getProperty("cvoc");
                    String targetId = Optional
                            .ofNullable(rel.<String>getProperty("concept"))
                            .orElseGet(() -> rel.getProperty("target"));

                    logger.debug(" - found link references: cvoc: {}, concept: {}", setId, targetId);
                    try {
                        AuthoritativeSet set = setCache.get(setId);
                        Optional<AuthoritativeItem> targetOpt = findTarget(set, targetId);
                        if (targetOpt.isPresent()) {
                            AuthoritativeItem target = targetOpt.get();
                            try {
                                Optional<Link> linkOpt = findLink(unit, target, rel, linkTemplate);
                                if (linkOpt.isPresent()) {
                                    logger.debug(" - found existing link created between {} and {}", targetId, target.getId());
                                } else {
                                    Link link = api.create(linkTemplate, Link.class);
                                    unit.addLink(link);
                                    target.addLink(link);
                                    link.addLinkBody(rel);
                                    logger.debug(" - new link created between {} and {}", targetId, target.getId());
                                    created++;
                                }
                            } catch (PermissionDenied | DeserializationError | SerializationError ex) {
                                logger.error("Unexpected error resolving link for " + setId + "/" + targetId, ex);
                            }
                        } else {
                            logger.warn(" - unable to find link target with id: {}", targetId);
                        }
                    } catch (ExecutionException ex) {
                        logger.warn(" - unable to find link set with id: {}", setId);
                    }
                }
            }
        }
        return created;
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
