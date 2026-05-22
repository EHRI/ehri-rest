/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
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

package eu.ehri.project.tools;

import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.PermissionUtils;
import eu.ehri.project.api.Api;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Class for migrating data from one item to another, in the case
 * where a unit has been replaced by another in the hierarchy.
 * <p>
 * The Persistent Identifier, access restrictions, and references
 * to the original item such as annotations, links will be migrated
 * from one item to the other.
 */
public class Migrator {
    private static final Logger logger = LoggerFactory.getLogger(Migrator.class);

    public static final String PID_PREFIX = "MOVED-";

    private final Api api;
    private final GraphManager manager;
    private final Serializer depSerializer;
    private final PermissionUtils helper;
    private final PermissionScope scope;

    public Migrator(FramedGraph<?> graph, Api api, PermissionScope scope) {
        this.api = api;
        this.scope = scope;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.depSerializer = api.serializer().withDependentOnly(true);
        this.helper = new PermissionUtils(graph, scope);
    }

    public void migrate(DocumentaryUnit from, DocumentaryUnit to, Actioner actioner)
            throws SerializationError, ItemNotFound, PermissionDenied {
        this.helper.checkEntityPermission(from, actioner.as(Accessor.class), PermissionType.UPDATE);
        this.helper.checkEntityPermission(from, actioner.as(Accessor.class), PermissionType.DELETE);
        this.helper.checkEntityPermission(to, actioner.as(Accessor.class), PermissionType.UPDATE);
        if (!Objects.equals(from.getRepository(), to.getRepository())) {
            throw new IllegalArgumentException("migration source and destination must share the same repository");
        }
        transferPersistentIdentifiers(from, to);
        transferUserGeneratedContent(from, to);
        transferAccessors(from, to);
    }

    private void transferPersistentIdentifiers(DocumentaryUnit from, DocumentaryUnit to) {
        String fromPid = from.getPersistentIdentifier();
        if (fromPid == null) {
            throw new IllegalStateException("Source item " + from.getId() + " has no PID");
        }
        if (fromPid.startsWith(PID_PREFIX)) {
            throw new IllegalArgumentException("Attempting to migrate an already-moved PID");
        }
        from.asVertex().setProperty(Ontology.PID_KEY, PID_PREFIX + fromPid);
        to.asVertex().setProperty(Ontology.PID_KEY, fromPid);
    }

    private void transferUserGeneratedContent(DocumentaryUnit from, DocumentaryUnit to) throws SerializationError, ItemNotFound {
        List<Link> links = Lists.newArrayList(from.getLinks());
        for (Link link : links) {
            if (link.getLinkBodies().iterator().hasNext()) {
                // Skip links with a body...
                continue;
            }
            logger.debug("Moving link from {} to {}...", from.getId(), to.getId());
            link.addLinkTarget(to);
            link.removeLinkTarget(from);
        }
        List<Annotation> annotations = Lists.newArrayList(from.getAnnotations());
        for (Annotation annotation : annotations) {
            logger.debug("Moving annotation from {} to {}...", from.getId(), to.getId());
            to.addAnnotation(annotation);
            from.removeAnnotation(annotation);
            for (Annotatable part : annotation.getTargetParts()) {
                findPart(part, to).ifPresent(altPart -> {
                    logger.debug("Found equivalent target part: {}", altPart.getId());
                    altPart.addAnnotationPart(annotation);
                    part.removeAnnotationPart(annotation);
                });
            }
        }
        List<VirtualUnit> inVc = Lists.newArrayList(from.getVirtualParents());
        for (VirtualUnit vc : inVc) {
            logger.debug("Moving VC membership from {} to {}", from.getId(), to.getId());
            vc.addIncludedUnit(to);
            vc.removeIncludedUnit(from);
        }
    }

    private void transferAccessors(DocumentaryUnit from, DocumentaryUnit to) {
        ArrayList<Accessor> accessors = Lists.newArrayList(from.getAccessors());
        if (!accessors.isEmpty()) {
            api.aclManager().setAccessors(to, accessors);
            logger.debug("Copying access control from {} to {}", from.getId(), to.getId());
        }
    }

    private Optional<Annotatable> findPart(Annotatable orig, DocumentaryUnit newParent)
            throws SerializationError, ItemNotFound {
        Bundle newParentBundle = depSerializer.entityToBundle(newParent);
        Bundle dep = depSerializer.entityToBundle(orig);

        BiFunction<Bundle, Bundle, Boolean> isEquivalentDescription =
                (Bundle a, Bundle b) -> Objects.equals(a.getType(), b.getType())
                        && Objects.equals(a.getDataValue(Ontology.LANGUAGE), b.getDataValue(Ontology.LANGUAGE))
                        && Objects.equals(a.getDataValue(Ontology.IDENTIFIER_KEY), b.getDataValue(Ontology.IDENTIFIER_KEY));

        Optional<Bundle> bundle = newParentBundle.find(b ->
                b.equals(dep) || isEquivalentDescription.apply(dep, b));
        return bundle.isPresent()
                ? Optional.of(manager.getEntity(bundle.get().getId(), Annotatable.class))
                : Optional.empty();
    }
}
